package de.steg0.deskapps.tabletool;

import java.sql.SQLException;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

/**
 * The model to make connections, and a specific selected index in the list,
 * available to {@link JdbcNotebookController}. 
 */
class ConnectionListModel
implements ComboBoxModel<Connections.ConnectionState>
{

    private final Connections connections;
    private Object selected;
    
    ConnectionListModel(Connections connections)
    {
        this.connections = connections;
    }

    /**blocking; establishes connection if needed */
    ConnectionWorker getConnection(Connections.ConnectionState connection)
    throws SQLException
    {
        return connections.getConnection(connection);
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
