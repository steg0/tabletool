package de.steg0.deskapps.tabletool;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

class ConnectionListModel
implements ComboBoxModel<String>
{
    static final boolean AUTOCOMMIT_DEFAULT=false;

    PropertyHolder propertyHolder;
    Executor executor;
    boolean autocommitDefault;
    PropertyHolder.ConnectionInfo[] connectionInfo;
    ConnectionWorker[] connections;
    Object selected;
    
    ConnectionListModel(PropertyHolder propertyHolder,Executor executor)
    {
        this.propertyHolder = propertyHolder;
        this.executor = executor;
        this.autocommitDefault = AUTOCOMMIT_DEFAULT;
        connectionInfo = propertyHolder.getConnections();
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
                jdbcConnection.setAutoCommit(autocommitDefault);
                connections[i] = new ConnectionWorker(
                        jdbcConnection,
                        executor
                );
            }
            return connections[i];
        }
        return null;
    }
    
    /**does not try to establish a connection; only returns non-null
     * result if one is already present. */
    ConnectionWorker selected()
    {
        Object name = getSelectedItem();
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

    @Override
    public int getSize()
    {
        return connectionInfo.length;
    }

    @Override
    public String getElementAt(int index)
    {
        return connectionInfo[index].name;
    }

    @Override
    public void addListDataListener(ListDataListener l)
    {
    }

    @Override
    public void removeListDataListener(ListDataListener l)
    {
    }

    @Override
    public void setSelectedItem(Object anItem)
    {
        selected = anItem;
    }

    @Override
    public Object getSelectedItem()
    {
        return selected;
    }
    
}
