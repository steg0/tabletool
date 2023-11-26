package de.steg0.deskapps.tabletool;

import java.sql.SQLException;
import java.util.function.Consumer;

import javax.swing.ComboBoxModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * The model to make connections, and a specific selected index in the list,
 * available to {@link NotebookController}. 
 */
class ConnectionListModel
implements ComboBoxModel<Connections.ConnectionState>
{

    private final Connections connections;
    private Object selected;
    private final EventListenerList listeners = new EventListenerList();
    
    ConnectionListModel(Connections connections)
    {
        this.connections = connections;
    }

    void notifyIntervalAdded(int oldSize)
    {
        for(ListDataListener l : listeners.getListeners(ListDataListener.class))
        {
            ListDataEvent event = new ListDataEvent(this,
                    ListDataEvent.INTERVAL_ADDED,oldSize,connections.getSize());
            l.intervalAdded(event);
        }
    }

    /**blocking; establishes connection if needed */
    ConnectionWorker getConnection(Connections.ConnectionState connection,
            Consumer<String> log)
    throws SQLException
    {
        return connections.getConnection(connection,log);
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
        this.listeners.add(ListDataListener.class,l);
    }

    @Override
    public void removeListDataListener(ListDataListener l)
    {
        this.listeners.remove(ListDataListener.class,l);
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
