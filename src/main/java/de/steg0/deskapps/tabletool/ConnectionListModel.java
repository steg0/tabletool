package de.steg0.deskapps.tabletool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

public class ConnectionListModel
implements ComboBoxModel<String>
{
    PropertyHolder propertyHolder;
    PropertyHolder.ConnectionInfo[] connectionInfo;
    Connection[] connections;
    Object selected;
    
    ConnectionListModel(PropertyHolder propertyHolder)
    {
        this.propertyHolder = propertyHolder;
        connectionInfo = propertyHolder.getConnections();
        connections = new Connection[connectionInfo.length];
    }
    
    /**blocking */
    Connection getConnection(Object name)
    throws SQLException
    {
        for(int i=0;i<connectionInfo.length;i++)
        {
            if(!connectionInfo[i].name.equals(name)) continue;
            if(connections[i] == null)
            {
                connections[i] = DriverManager.getConnection(
                        connectionInfo[i].url,
                        connectionInfo[i].username,
                        connectionInfo[i].password
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
