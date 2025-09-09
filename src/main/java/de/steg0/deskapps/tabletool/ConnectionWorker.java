package de.steg0.deskapps.tabletool;

import static javax.swing.SwingUtilities.invokeAndWait;
import static javax.swing.SwingUtilities.invokeLater;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import de.steg0.deskapps.tabletool.PropertyHolder.ConnectionInfo;

/**
 * A wrapper to run operations on top of a JDBC <code>Connection</code>.
 */
class ConnectionWorker
{
    Logger logger = Logger.getLogger("tabtype");

    final ConnectionInfo info;

    private final Connection connection;
    private final ThreadPoolExecutor executor;
    
    ConnectionWorker(ConnectionInfo info,Connection connection,
            ThreadPoolExecutor executor)
    {
        this.info=info;
        this.connection=connection;
        this.executor=executor;
    }

    private void report(Consumer<String> log,String str)
    {
        log.accept(str);
    }

    ResultSetTableModel lastReportedResult;
    volatile Statement lastStatement;
    
    /**
     * @param resultConsumer where a Statement will be pushed to right after
     * <code>execute()</code> on the Swing event thread, if
     * <code>execute()</code> returned <code>true</code> 
     * @param resultConsumer where an update count will be pushed to right
     * after <code>execute()</code> on the Swing event thread, if
     * <code>execute()</code> returned <code>false</code> 
     * @param log where log messages will be pushed to as far as available. This
     * is not necessarily called from the event thread.
     */
    void submit(
            String sql,
            int fetchsize,
            JdbcParametersInputController parametersController,
            String placeholderlog,
            BiConsumer<ResultSetTableModel,Long> resultConsumer,
            Consumer<UpdateCountEvent> updateCountConsumer,
            Consumer<String> log,
            boolean skipEmptyColumns,
            boolean updatable
    )
    {
        if(lastStatement!=null)
        {
            report(log,"Currently executing:\n"+lastStatement+
                    "\nNot enqueueing new statement at "+new Date());
            return;
        }
        logger.info(sql);
        logger.log(Level.FINE,"Using {0}",info.url);
        var sqlwrapper = new SqlOperationWrapper();
        sqlwrapper.parametersController = parametersController;
        sqlwrapper.resultConsumer = resultConsumer;
        sqlwrapper.updateCountConsumer = updateCountConsumer;
        sqlwrapper.fetchsize = fetchsize;
        sqlwrapper.log = log;
        sqlwrapper.sql = sql;
        sqlwrapper.skipEmptyColumns = skipEmptyColumns;
        sqlwrapper.updatable = updatable;
        sqlwrapper.placeholderlog = placeholderlog;
        sqlwrapper.ts = System.currentTimeMillis();
        sqlwrapper.start();
    }
    
    private class SqlOperationWrapper
    {
        private JdbcParametersInputController parametersController;
        private BiConsumer<ResultSetTableModel,Long> resultConsumer;
        private Consumer<UpdateCountEvent> updateCountConsumer;
        private Consumer<String> log;
        private boolean skipEmptyColumns,updatable;
        private String sql;
        private String placeholderlog;
        private int fetchsize;
        private long ts;

        public void start()
        {
            executeOnConnection("SQL",sql,() ->
            {
                if(lastReportedResult!=null && !lastReportedResult.isClosed())
                {
                    lastReportedResult.close();
                    logger.fine("Closed prior ResultSet");
                }
                getResult();
                return null;
            },log);
        }

        private void getResult()
        throws Exception
        {
            try
            {
                Matcher callableStatementParts = CallableStatementMatchers
                        .prefixMatch(sql);
                String lc = sql.toLowerCase();
                String inlog,outlog;
                if(callableStatementParts.group(1).length() > 0)
                {
                    if(sql.endsWith(";"))
                    {
                        String noSemicolon = lc.substring(0,sql.length()-1);
                        if(!noSemicolon.trim().endsWith("end")) sql = sql
                                .substring(0,sql.length()-1);
                    }

                    CallableStatement st = connection.prepareCall(sql);

                    inlog = parameterTransfer(st,
                            JdbcParametersInputController::applyToStatement);

                    logger.fine("Executing (callable statement)");
                    boolean result = st.execute();

                    lastStatement = st;
                    outlog = parameterTransfer(st,
                            JdbcParametersInputController::readFromStatement);

                    if(result)
                    {
                        reportResult(st,inlog,outlog,false);
                    }
                    else
                    {
                        reportNullResult();
                        displayUpdateCount(st,inlog,outlog);
                    }
                }
                else
                {
                    if(sql.endsWith(";")) sql = sql.substring(0,sql.length()-1);
                    PreparedStatement st = updatable?
                            connection.prepareStatement(
                                    sql,
                                    ResultSet.TYPE_FORWARD_ONLY,
                                    ResultSet.CONCUR_UPDATABLE
                            ) :
                            connection.prepareStatement(
                                    sql
                            );

                    inlog = parameterTransfer(st,
                            JdbcParametersInputController::applyToStatement);

                    logger.fine("Executing (statement)");
                    lastStatement = st;
                    boolean result = st.execute();

                    outlog = parameterTransfer(st,
                            JdbcParametersInputController::readFromStatement);

                    if(result)
                    {
                        reportResult(st,inlog,outlog,updatable);
                    }
                    else
                    {
                        reportNullResult();
                        displayUpdateCount(st,inlog,outlog);
                    }
                }
            }
            catch(SQLException e)
            {
                reportNullResult();
                throw e;
            }
            finally
            {
                lastStatement = null;
            }
        }

        private interface ParameterControllerFunction
        {
            String apply(JdbcParametersInputController controller,
                    PreparedStatement st) throws SQLException;
        }
        
        /**Applies <code>f</code> to <code>st</code> in the event thread. */
        private String parameterTransfer(PreparedStatement st,
                ParameterControllerFunction f)
        throws Exception
        {
            class SwingRunnable implements Runnable
            {
                SQLException e;
                String report;
                
                public void run()
                {
                    try
                    {
                        if(parametersController!=null) report = f.apply(
                            parametersController,st);
                    }
                    catch(SQLException e)
                    {
                        this.e = e;
                    }
                }
            }
            var r = new SwingRunnable();
            invokeAndWait(r);
            if(r.e != null) throw r.e;
            return r.report;
        }

        private void displayUpdateCount(Statement statement,String inlog,
                String outlog)
        throws SQLException
        {
            long now = System.currentTimeMillis();
            var countEvent = new UpdateCountEvent(ConnectionWorker.this,
                    statement.getUpdateCount(),now-ts,inlog,outlog,
                    placeholderlog);
            invokeLater(() -> updateCountConsumer.accept(countEvent));
            statement.close();
        }
        
        private void reportNullResult()
        {
            invokeLater(() -> resultConsumer.accept(null,0L));
        }

        private void reportResult(Statement statement,String inlog,
                String outlog,boolean updatable)
        throws SQLException
        {
            lastReportedResult = new ResultSetTableModel();
            lastReportedResult.update(info.name,statement,fetchsize,inlog,
                    outlog,placeholderlog,skipEmptyColumns,updatable);
            long now = System.currentTimeMillis();
            invokeLater(() -> resultConsumer.accept(lastReportedResult,
                    now-ts));
        }
    }

    /**
     * This is not needed before <code>submit</code> where it's done
     * automatically.
     */
    void closeResultSet(Consumer<String> log)
    {
        executeOnConnection("Close ResultSet",null,() -> 
        {
            if(lastReportedResult!=null)
            {
                lastReportedResult.close();
                lastReportedResult = null;
                return "Closed ResultSet";
            }
            return null;
        },log);
    }

    void cancel()
    throws SQLException
    {
        Statement st = lastStatement;
        if(st!=null)
        {
            st.cancel();
        }
    }
    
    void commit(Consumer<String> log)
    {
        executeOnConnection("Commit",null,() ->
        {
            connection.commit();
            return "Commit complete";
        },log);
    }
    
    void rollback(Consumer<String> log)
    {
        executeOnConnection("Rollback",null,() ->
        {
            connection.rollback();
            return "Rollback complete";
        },log);
    }
    
    void disconnect(Consumer<String> log,Runnable cb)
    {
        executeOnConnection("Disconnect",null,() ->
        {
            connection.close();
            invokeLater(cb);
            return "Disconnected";
        },log);
    }
    
    void setAutoCommit(boolean enabled,Consumer<String> log,Runnable cb)
    {
        executeOnConnection("Enable autocommit",null,() ->
        {
            connection.setAutoCommit(enabled);
            invokeLater(cb);
            return "Autocommit set to "+enabled+". Use Ctrl+ENTER, "+
                    "Ctrl+R, or F5 to execute statements";
        },log);
    }

    private void executeOnConnection(String operationName,String statement,
            Callable<String> runnable,Consumer<String> log)
    {
        executor.execute(new Operation(operationName,statement,runnable,log));
    }

    class Operation implements Runnable
    {
        private Consumer<String> log;
        private String name,sql;
        private Callable<String> callable;
        private Date start=new Date();

        Operation(String name,String sql,Callable<String> callable,
                Consumer<String> log)
        {
            this.name=name;
            this.callable=callable;
            this.log=log;
            this.sql=sql;
        }

        ConnectionInfo connectionInfo()
        {
            return info;
        }

        @Override public void run()
        {
            synchronized(ConnectionWorker.this)
            {
                try
                {
                    String reportmsg = "Accepted: "+name+" at "+start;
                    report(log,reportmsg);
                    String resultMessage = callable.call();
                    if(resultMessage != null)
                    {
                        report(log,resultMessage + " at " + new Date() + ".");
                    }
                    logger.fine("Done: "+name);
                }
                catch(SQLException e)
                {
                    if(sql!=null) report(log,SQLExceptionPrinter.toString(sql,
                            e));
                    else report(log,SQLExceptionPrinter.toString(e));
                    logger.log(Level.INFO,"SQLException on {0}",info.url);
                }
                catch(Exception e)
                {
                    report(log,"Internal error: " + e.getMessage() + " at " + 
                            new Date());
                    logger.log(Level.SEVERE,"Internal error",e);
                }
            }
        }

        @Override public String toString()
        {
            return name + " at " + start + " on " + info.name;
        }
    }
}