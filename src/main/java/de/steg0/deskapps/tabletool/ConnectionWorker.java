package de.steg0.deskapps.tabletool;

import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.SwingUtilities.invokeAndWait;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.concurrent.Executor;
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
    private final Executor executor;
    
    ConnectionWorker(ConnectionInfo info,Connection connection,Executor executor)
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
        logger.info(sql);
        var sqlRunnable = new SqlRunnable();
        sqlRunnable.parametersController = parametersController;
        sqlRunnable.resultConsumer = resultConsumer;
        sqlRunnable.updateCountConsumer = updateCountConsumer;
        sqlRunnable.fetchsize = fetchsize;
        sqlRunnable.log = log;
        sqlRunnable.sql = sql;
        sqlRunnable.skipEmptyColumns = skipEmptyColumns;
        sqlRunnable.updatable = updatable;
        sqlRunnable.placeholderlog = placeholderlog;
        sqlRunnable.ts = System.currentTimeMillis();
        executor.execute(sqlRunnable);
    }
    
    private class SqlRunnable implements Runnable
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

        public void run()
        {
            synchronized(ConnectionWorker.this)
            {
                try
                {
                    if(lastReportedResult!=null && 
                       !lastReportedResult.isClosed()) try
                    {
                        lastReportedResult.close();
                        report(log,"Closed prior ResultSet and accepted query "+
                                "at "+new Date());
                    }
                    catch(SQLException e)
                    {
                        report(log,SQLExceptionPrinter.toString(e));
                    }
                    else
                    {
                        report(log,"Query accepted at "+new Date());
                    }
                    getResult();
                }
                catch(SQLException e)
                {
                    report(log,SQLExceptionPrinter.toString(sql,e));
                }
                catch(Exception e)
                {
                    report(log,"Internal error: " + e.getMessage() + " at " + 
                            new Date());
                    logger.log(Level.SEVERE, "Internal error",e);
                }
            }
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

                    boolean result = st.execute();

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
            };
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
        executor.execute(() ->
        {
            synchronized(ConnectionWorker.this)
            {
                if(lastReportedResult!=null) try
                {
                    lastReportedResult.close();
                    lastReportedResult = null;
                    report(log,"Closed ResultSet at "+new Date());
                }
                catch(SQLException e)
                {
                    report(log,SQLExceptionPrinter.toString(e));
                }
            }
        });
    }
    
    void commit(Consumer<String> log)
    {
        executor.execute(() ->
        {
            synchronized(ConnectionWorker.this)
            {
                try
                {
                    connection.commit();
                    report(log,"Commit complete at "+new Date());
                }
                catch(SQLException e)
                {
                    report(log,SQLExceptionPrinter.toString(e));
                }
            }
        });
    }
    
    void rollback(Consumer<String> log)
    {
        executor.execute(() ->
        {
            synchronized(ConnectionWorker.this)
            {
                try
                {
                    connection.rollback();
                    report(log,"Rollback complete at "+new Date());
                }
                catch(SQLException e)
                {
                    report(log,SQLExceptionPrinter.toString(e));
                }
            }
        });
    }
    
    void disconnect(Consumer<String> log,Runnable cb)
    {
        executor.execute(() ->
        {
            synchronized(ConnectionWorker.this)
            {
                try
                {
                    connection.close();
                    report(log,"Disconnected at "+new Date());
                    invokeLater(cb);
                }
                catch(SQLException e)
                {
                    report(log,SQLExceptionPrinter.toString(e));
                }
            }
        });
    }
    
    void setAutoCommit(boolean enabled,Consumer<String> log,Runnable cb)
    {
        executor.execute(() ->
        {
            synchronized(ConnectionWorker.this)
            {
                try
                {
                    connection.setAutoCommit(enabled);
                    report(log,"Autocommit set to "+enabled+" at "+new Date()+
                            ". Use Ctrl+ENTER to execute statements.");
                    invokeLater(cb);
                }
                catch(SQLException e)
                {
                    report(log,SQLExceptionPrinter.toString(e));
                }
            }
        });
    }
}
