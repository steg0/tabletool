package de.steg0.deskapps.tabletool;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executor;

class Connections
{
    static final boolean AUTOCOMMIT_DEFAULT=false;

    PropertyHolder.ConnectionInfo[] connectionInfo;
    ConnectionWorker[] connections;
    Executor executor;
    
    Connections(PropertyHolder propertyHolder,Executor executor)
    {
        connectionInfo = propertyHolder.getConnections();
        this.executor = executor;
        connections = new ConnectionWorker[connectionInfo.length];
    }
    
    /**blocking; establishes connection if needed */
    ConnectionWorker getConnection(Object name)
    throws SQLException
    {
        for(int i=0;i<connectionInfo.length;i++)
        {
            if(!connectionInfo[i].name.equals(name)) continue;
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
        return null;
    }
    
    ConnectionWorker getIfConnected(Object name)
    {
        for(int i=0;i<connectionInfo.length;i++)
        {
            if(!connectionInfo[i].name.equals(name)) continue;
            return connections[i];
        }
        return null;
    }
    
    void reportDisconnect(ConnectionWorker connection)
    {
        for(int i=0;i<connectionInfo.length;i++)
        {
            if(connections[i] != connection) continue;
            connections[i] = null;
        }
    }

    public int getSize()
    {
        return connectionInfo.length;
    }

    public String getElementAt(int index)
    {
        return connectionInfo[index].name;
    }
}
