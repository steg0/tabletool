package de.steg0.deskapps.tabletool;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

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
        int connectionIndex;
        
        PropertyHolder.ConnectionInfo info()
        {
            return connectionInfo[connectionIndex];
        }
        
        public String toString()
        {
            /* connected ones get a star in front */
            return (connections[connectionIndex] != null? "*" : "  ") +
                    connectionInfo[connectionIndex].name;
        }
    }

    final ConnectionState[] connectionState;
    final PropertyHolder.ConnectionInfo[] connectionInfo;
    final ConnectionWorker[] connections;
    final Executor executor;
    final Pattern callablePattern;
    
    Connections(PropertyHolder propertyHolder,Executor executor,
            Pattern callablePattern)
    {
        connectionInfo = propertyHolder.getConnections();
        this.executor = executor;
        connections = new ConnectionWorker[connectionInfo.length];
        connectionState = new ConnectionState[connectionInfo.length];
        this.callablePattern = callablePattern;

        for(int i=0;i<connectionInfo.length;i++)
        {
            var state = new ConnectionState();
            state.connectionIndex = i;
            connectionState[i] = state;
        }
    }
    
    /**blocking; establishes connection if needed */
    ConnectionWorker getConnection(ConnectionState connection)
    throws SQLException
    {
        int i = connection.connectionIndex;
        if(connections[i] == null)
        {
            var jdbcConnection = DriverManager.getConnection(
                    connectionInfo[i].url,
                    connectionInfo[i].username,
                    connectionInfo[i].password
            );
            jdbcConnection.setAutoCommit(AUTOCOMMIT_DEFAULT);
            connections[i] = new ConnectionWorker(
                    jdbcConnection,
                    executor,
                    callablePattern
            );
        }
        return connections[i];
    }
    
    void reportDisconnect(ConnectionWorker connection)
    {
        for(int i=0;i<connectionInfo.length;i++)
        {
            if(connections[i] != connection) continue;
            connections[i] = null;
        }
    }

    int getSize()
    {
        return connectionInfo.length;
    }

    ConnectionState getElementAt(int index)
    {
        return connectionState[index];
    }
}
