package de.steg0.deskapps.tabletool;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Represents the list of connections available to the runtime.
 */
class Connections
{
    static final boolean AUTOCOMMIT_DEFAULT=false;

    /**
     * Exposes an index into the connection list.
     */
    class ConnectionState
    {
        private int connectionIndex;
        
        PropertyHolder.ConnectionInfo info()
        {
            return connectionInfo[connectionIndex];
        }
        
        public String toString()
        {
            /* connected ones get a star in front */
            return (workers[connectionIndex] != null? "*" : "  ") +
                    connectionInfo[connectionIndex].name;
        }
    }

    private ConnectionState[] connectionStates;
    private PropertyHolder.ConnectionInfo[] connectionInfo;
    private ConnectionWorker[] workers;
    private final Executor executor;
    
    Connections(PropertyHolder propertyHolder,Executor executor)
    {
        connectionInfo = propertyHolder.getConnections();
        this.executor = executor;
        workers = new ConnectionWorker[connectionInfo.length];
        initStates();
    }

    /**
     * Refreshes the list of available connection definitions. New ones
     * are added to the end of the list. Removed ones are kept to not
     * require disconnect.
     */
    void refresh(PropertyHolder propertyHolder)
    {
        var newConnectionInfo = propertyHolder.getConnections();
        List<PropertyHolder.ConnectionInfo> mergedConnectionInfo =
            new ArrayList<>();
        for(var info : connectionInfo) mergedConnectionInfo.add(info);
        for(var info : newConnectionInfo)
        {
            int existingIndex = mergedConnectionInfo.indexOf(info);
            if(existingIndex >= 0)
            {
                mergedConnectionInfo.set(existingIndex, info);
            }
            else
            {
                mergedConnectionInfo.add(info);
            }
        }
        connectionInfo = mergedConnectionInfo.toArray(
                new PropertyHolder.ConnectionInfo[mergedConnectionInfo.size()]);
        var newWorkers = new ConnectionWorker[connectionInfo.length];
        System.arraycopy(workers,0,newWorkers,0,workers.length);
        workers = newWorkers;
        initStates();
    }

    private void initStates()
    {
        connectionStates = new ConnectionState[connectionInfo.length];

        for(int i=0;i<connectionInfo.length;i++)
        {
            var state = new ConnectionState();
            state.connectionIndex = i;
            connectionStates[i] = state;
        }
    }
    
    /**blocking; establishes connection if needed */
    ConnectionWorker getConnection(ConnectionState connection,
            Consumer<String> log)
    throws SQLException
    {
        int i = connection.connectionIndex;
        if(workers[i] == null)
        {
            var jdbcConnection = DriverManager.getConnection(
                    connectionInfo[i].url,
                    connectionInfo[i].username,
                    connectionInfo[i].password
            );
            jdbcConnection.setAutoCommit(AUTOCOMMIT_DEFAULT);
            workers[i] = new ConnectionWorker(
                    connectionInfo[i],
                    jdbcConnection,
                    executor
            );
            if(connectionInfo[i].initSql != null)
            {
                workers[i].submit(connectionInfo[i].initSql,0,null,null,
                        (r,c) -> {},e -> {},log);
            }
        }
        return workers[i];
    }
    
    void reportDisconnect(ConnectionWorker connection)
    {
        for(int i=0;i<connectionInfo.length;i++)
        {
            if(workers[i] != connection) continue;
            workers[i] = null;
        }
    }

    boolean isConnected(ConnectionState connection)
    {
        return workers[connection.connectionIndex] != null;
    }

    int getSize()
    {
        return connectionInfo.length;
    }

    ConnectionState getElementAt(int index)
    {
        return connectionStates[index];
    }
}
