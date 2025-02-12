package de.steg0.deskapps.tabletool;

import static javax.swing.SwingUtilities.invokeLater;

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
            BiConsumer<ResultSetTableModel,Long> resultConsumer,
            Consumer<UpdateCountEvent> updateCountConsumer,
            Consumer<String> log
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
        sqlRunnable.ts = System.currentTimeMillis();
        executor.execute(sqlRunnable);
    }
    
    private class SqlRunnable implements Runnable
    {
        private JdbcParametersInputController parametersController;
        private BiConsumer<ResultSetTableModel,Long> resultConsumer;
        private Consumer<UpdateCountEvent> updateCountConsumer;
        private Consumer<String> log;
        private String sql;
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
                    getResult(sql);
                }
                catch(SQLException e)
                {
                    report(log,SQLExceptionPrinter.toString(sql,e));
                }
            }
        }

        private void getResult(String text)
        throws SQLException
        {
            try
            {
                Matcher callableStatementParts = CallableStatementMatchers
                        .prefixMatch(text);
                String lc = text.toLowerCase();
                if(callableStatementParts.group(1).length() > 0)
                {
                    if(text.endsWith(";"))
                    {
                        String noSemicolon = lc.substring(0,text.length()-1);
                        if(!noSemicolon.trim().endsWith("end")) text = text
                                .substring(0,text.length()-1);
                    }

                    CallableStatement st = connection.prepareCall(text);

                    if(parametersController != null)
                        parametersController.applyToStatement(st);

                    boolean update = st.execute();

                    if(parametersController != null)
                        parametersController.readFromStatement(st);

                    if(update)
                    {
                        reportResult(st);
                    }
                    else
                    {
                        reportNullResult();
                        displayUpdateCount(st);
                    }
                }
                else
                {
                    if(text.endsWith(";")) text =
                            text.substring(0,text.length()-1);
                    PreparedStatement st = connection.prepareStatement(
                            text,
                            ResultSet.TYPE_FORWARD_ONLY,
                            ResultSet.CONCUR_UPDATABLE
                    );

                    if(parametersController != null)
                        parametersController.applyToStatement(st);

                    boolean update = st.execute();

                    if(parametersController != null)
                        parametersController.readFromStatement(st);

                    if(update)
                    {
                        reportResult(st);
                    }
                    else
                    {
                        reportNullResult();
                        displayUpdateCount(st);
                    }
                }
            }
            catch(SQLException e)
            {
                reportNullResult();
                throw e;
            }
        }
        
        private void displayUpdateCount(Statement statement)
        throws SQLException
        {
            long now = System.currentTimeMillis();
            var countEvent = new UpdateCountEvent(ConnectionWorker.this,
                    statement.getUpdateCount(), now-ts);
            invokeLater(() -> updateCountConsumer.accept(countEvent));
            statement.close();
        }
        
        private void reportNullResult()
        {
            invokeLater(() -> resultConsumer.accept(null,0L));
        }

        private void reportResult(Statement statement)
        throws SQLException
        {
            lastReportedResult = new ResultSetTableModel();
            lastReportedResult.update(info.name,statement,fetchsize);
            long now = System.currentTimeMillis();
            invokeLater(() -> resultConsumer.accept(lastReportedResult,
                    now-ts));
        }
    }

    /**
     * This is not needed before
     * {@link #submit(String,int,Consumer,Consumer)} where it's done
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
