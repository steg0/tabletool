package de.steg0.deskapps.tabletool;

import java.sql.SQLException;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

class ConnectionListModel
implements ComboBoxModel<Connections.ConnectionState>
{

    Connections connections;
    Object selected;
    
    ConnectionListModel(Connections connections)
    {
        this.connections = connections;
    }

    /**blocking; establishes connection if needed */
    ConnectionWorker getConnection(Object connection)
    throws SQLException
    {
        return connections.getConnection(connection);
    }
    
    void reportDisconnect(ConnectionWorker connection)
    {
        connections.reportDisconnect(connection);
    }

    @Override
    public int getSize()
    {
        return connections.getSize();
    }

    @Override
    public Connections.ConnectionState getElementAt(int index)
    {
        return connections.getElementAt(index);
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
