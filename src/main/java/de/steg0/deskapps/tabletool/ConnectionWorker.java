package de.steg0.deskapps.tabletool;

import static javax.swing.SwingUtilities.invokeLater;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
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
    
    /**
     * @param resultConsumer where a Statement will be pushed to right after
     * <code>execute()</code> om the Swing event thread 
     * on the Swing event thread once available
     * @param log where log messages will be pushed to on the Swing event thread
     * as far as available
     */
    void submit(
            String sql,
            Consumer<Statement> resultConsumer,
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
        Consumer<Statement> resultConsumer;
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
                    invokeLater(() -> log.accept(SQLExceptionPrinter.toString(e)));
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
            invokeLater(() -> log.accept(
                    UPDATE_LOG_FORMAT.format(updateCount)
            ));
        }
        
        void reportResult(Statement statement)
        {
            invokeLater(() -> resultConsumer.accept(statement));
        }
    }
}
