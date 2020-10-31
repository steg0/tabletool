package de.steg0.deskapps.tabletool;

import static javax.swing.SwingUtilities.invokeLater;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

class ConnectionWorker
{
    final Connection connection;
    final Executor executor;
    
    ConnectionWorker(Connection connection,Executor executor)
    {
        this.connection=connection;
        this.executor=executor;
    }
    
    void report(Consumer<String> log,String str)
    {
        invokeLater(() -> log.accept(str));
    }
    
    ResultSetTableModel lastReportedResult;
    
    /**
     * @param resultConsumer where a Statement will be pushed to right after
     * <code>execute()</code> om the Swing event thread, if execute() returned
     * <code>true</code> 
     * @param resultConsumer where an update count will be pushed to right after
     * <code>execute()</code> om the Swing event thread, if execute() returned
     * <code>false</code> 
     * on the Swing event thread once available
     * @param log where log messages will be pushed to on the Swing event thread
     * as far as available
     */
    void submit(
            String sql,
            int fetchsize,
            Consumer<ResultSetTableModel> resultConsumer,
            Consumer<Integer> updateCountConsumer,
            Consumer<String> log
    )
    {
        var sqlRunnable = new SqlRunnable();
        sqlRunnable.resultConsumer = resultConsumer;
        sqlRunnable.updateCountConsumer = updateCountConsumer;
        sqlRunnable.fetchsize = fetchsize;
        sqlRunnable.log = log;
        sqlRunnable.sql = sql;
        executor.execute(sqlRunnable);
    }
    
    class SqlRunnable implements Runnable
    {
        Consumer<ResultSetTableModel> resultConsumer;
        Consumer<Integer> updateCountConsumer;
        Consumer<String> log;
        String sql;
        int fetchsize;

        public void run()
        {
            synchronized(ConnectionWorker.this)
            {
                try
                {
                    if(lastReportedResult!=null) try
                    {
                        lastReportedResult.close();
                        report(log,"Closed prior ResultSet at "+new Date());
                    }
                    catch(SQLException e)
                    {
                        report(log,SQLExceptionPrinter.toString(e));
                    }
                    getResult(sql);
                }
                catch(SQLException e)
                {
                    report(log,SQLExceptionPrinter.toString(e));
                }
            }
        }

        void getResult(String text)
        throws SQLException
        {
            String lc = text.toLowerCase();
            if(lc.startsWith("begin") || 
               lc.startsWith("declare") ||
               lc.startsWith("create") ||
               lc.startsWith("{")
            )
            {
                if(text.endsWith(";"))
                {
                    String noSemicolon = lc.substring(0,text.length()-1);
                    if(!noSemicolon.trim().endsWith("end")) text = text
                            .substring(0,text.length()-1);
                }
                CallableStatement st = connection.prepareCall(text);
                if(st.execute()) reportResult(st);
                else displayUpdateCount(st);
            }
            else
            {
                Statement st = connection.createStatement(
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_UPDATABLE
                );
                if(text.endsWith(";")) text = text.substring(0,text.length()-1);
                if(st.execute(text)) reportResult(st);
                else displayUpdateCount(st);
            }
        }
        
        void displayUpdateCount(Statement statement)
        throws SQLException
        {
            Integer count = statement.getUpdateCount();
            invokeLater(() -> updateCountConsumer.accept(count));
            statement.close();
        }
        
        void reportResult(Statement statement)
        throws SQLException
        {
            lastReportedResult = new ResultSetTableModel();
            lastReportedResult.update(statement,fetchsize);
            invokeLater(() -> resultConsumer.accept(lastReportedResult));
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
    
    void disconnect(Consumer<String> log)
    {
        executor.execute(() ->
        {
            synchronized(ConnectionWorker.this)
            {
                try
                {
                    connection.close();
                    report(log,"Disconnected at "+new Date());
                }
                catch(SQLException e)
                {
                    report(log,SQLExceptionPrinter.toString(e));
                }
            }
        });
    }
    
    void setAutoCommit(boolean enabled,Consumer<String> log)
    {
        executor.execute(() ->
        {
            synchronized(ConnectionWorker.this)
            {
                try
                {
                    connection.setAutoCommit(enabled);
                    report(log,"Autocommit set to "+enabled+" at "+
                            new Date());
                }
                catch(SQLException e)
                {
                    report(log,SQLExceptionPrinter.toString(e));
                }
            }
        });
    }
}
