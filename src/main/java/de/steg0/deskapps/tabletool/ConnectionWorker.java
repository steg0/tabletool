package de.steg0.deskapps.tabletool;

import static javax.swing.SwingUtilities.invokeLater;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

class ConnectionWorker
{
    static final MessageFormat UPDATE_LOG_FORMAT = 
            new MessageFormat("{0,choice,-1#0 rows|0#0 rows|1#1 row|1<{0} rows} affected.\n");

    Connection connection;
    Executor executor;
    
    ConnectionWorker(Connection connection,Executor executor)
    {
        this.connection=connection;
        this.executor=executor;
    }
    
    void report(Consumer<String> log,String str)
    {
        invokeLater(() -> log.accept(str));
    }
    
    /**
     * @param resultConsumer where a Statement will be pushed to right after
     * <code>execute()</code> om the Swing event thread 
     * on the Swing event thread once available
     * @param log where log messages will be pushed to on the Swing event thread
     * as far as available
     */
    void submit(
            String sql,
            Consumer<ResultSetTableModel> resultConsumer,
            Consumer<String> log
    )
    {
        var sqlRunnable = new SqlRunnable();
        sqlRunnable.resultConsumer = resultConsumer;
        sqlRunnable.log = log;
        sqlRunnable.sql = sql;
        executor.execute(sqlRunnable);
    }
    
    class SqlRunnable implements Runnable
    {
        Consumer<ResultSetTableModel> resultConsumer;
        Consumer<String> log;
        String sql;

        public void run()
        {
            synchronized(ConnectionWorker.this)
            {
                try
                {
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
            if(text.startsWith("begin") || 
               text.startsWith("declare") ||
               text.startsWith("{")
            )
            {
                CallableStatement st = connection.prepareCall(text);
                if(st.execute()) reportResult(st);
                else displayUpdateCount(st);
            }
            else
            {
                Statement st = connection.createStatement();
                if(text.endsWith(";")) text = text.substring(0,text.length()-1);
                if(st.execute(text)) reportResult(st);
                else displayUpdateCount(st);
            }
        }
        
        void displayUpdateCount(Statement statement)
        throws SQLException
        {
            Object[] updateCount = {statement.getUpdateCount()};
            report(log,UPDATE_LOG_FORMAT.format(updateCount));
        }
        
        void reportResult(Statement statement)
        throws SQLException
        {
            var rsm = new ResultSetTableModel();
            rsm.update(statement);
            invokeLater(() -> resultConsumer.accept(rsm));
        }
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
    
    void setAutoCommit(boolean enabled,Consumer<String> log)
    {
        executor.execute(() ->
        {
            synchronized(ConnectionWorker.this)
            {
                try
                {
                    connection.setAutoCommit(enabled);
                    report(log,"Autocommit set to "+enabled+" at "+new Date());
                }
                catch(SQLException e)
                {
                    report(log,SQLExceptionPrinter.toString(e));
                }
            }
        });
    }
}
