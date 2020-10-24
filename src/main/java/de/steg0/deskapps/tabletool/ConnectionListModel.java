package de.steg0.deskapps.tabletool;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

class ConnectionListModel
implements ComboBoxModel<String>
{
    PropertyHolder propertyHolder;
    Executor executor;
    PropertyHolder.ConnectionInfo[] connectionInfo;
    ConnectionWorker[] connections;
    Object selected;
    
    ConnectionListModel(PropertyHolder propertyHolder,Executor executor)
    {
        this.propertyHolder = propertyHolder;
        this.executor = executor;
        connectionInfo = propertyHolder.getConnections();
        connections = new ConnectionWorker[connectionInfo.length];
    }
    
    /**blocking */
    ConnectionWorker getConnection(Object name)
    throws SQLException
    {
        for(int i=0;i<connectionInfo.length;i++)
        {
            if(!connectionInfo[i].name.equals(name)) continue;
            if(connections[i] == null)
            {
                connections[i] = new ConnectionWorker(
                        DriverManager.getConnection(
                                connectionInfo[i].url,
                                connectionInfo[i].username,
                                connectionInfo[i].password
                        ),
                        executor
                );
            }
            return connections[i];
        }
        return null;
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
