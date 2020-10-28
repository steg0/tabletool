package de.steg0.deskapps.tabletool;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executor;

class Connections
{
    static final boolean AUTOCOMMIT_DEFAULT=false;

    class ConnectionState
    {
        int connectionIndex;
        
        public String toString()
        {
            /* connected ones get a star in front */
            return (connections[connectionIndex] != null? "*" : "") +
                    connectionInfo[connectionIndex].name;
        }
    }

    ConnectionState[] connectionState;
    PropertyHolder.ConnectionInfo[] connectionInfo;
    ConnectionWorker[] connections;
    Executor executor;
    
    Connections(PropertyHolder propertyHolder,Executor executor)
    {
        connectionInfo = propertyHolder.getConnections();
        this.executor = executor;
        connections = new ConnectionWorker[connectionInfo.length];
        connectionState = new ConnectionState[connectionInfo.length];

        for(int i=0;i<connectionInfo.length;i++)
        {
            var state = new ConnectionState();
            state.connectionIndex = i;
            connectionState[i] = state;
        }
    }
    
    /**blocking; establishes connection if needed */
    ConnectionWorker getConnection(Object connection)
    throws SQLException
    {
        int i = ((ConnectionState)connection).connectionIndex;
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
                    executor
            );
        }
        return connections[i];
    }
    
    ConnectionWorker getIfConnected(Object connection)
    {
        return connections[((ConnectionState)connection).connectionIndex];
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
