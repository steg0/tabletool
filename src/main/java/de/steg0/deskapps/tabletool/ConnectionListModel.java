package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.function.Consumer;

import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import de.steg0.deskapps.tabletool.PropertyHolder.ConnectionInfo;

/**
 * The model to make connections, and a specific selected index in the list,
 * available to {@link NotebookController}. It can also show a password
 * dialog if a connection is configured for prompted passwords.
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

    /**
     * blocking; establishes connection if needed. Note: this checks if a
     * password has the special value "PROMPT", in which case it asks
     * the user for a password and sets it in the connection info. This
     * remains there until the next property refresh.
     */
    ConnectionWorker getConnection(Connections.ConnectionState connection,
            Consumer<String> log,JFrame parent)
    throws SQLException
    {
        ConnectionInfo info = connection.info();
        String oldPassword = info.password;
        if("PROMPT".equals(info.password))
        {
            info.password = null;
            var pf = new JPasswordField();
            var dialog = new JDialog(parent,"Enter password",true);
            dialog.getContentPane().add(new JLabel(
                    "Password for "+info.name+":"),BorderLayout.NORTH);
            dialog.getContentPane().add(pf,BorderLayout.CENTER);
            var okButton = new JButton("OK");
            dialog.setLocationRelativeTo(parent);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            ((BorderLayout)dialog.getContentPane().getLayout()).setVgap(5);
            okButton.addActionListener(e ->
            {
                info.password=new String(pf.getPassword());
                dialog.dispose();
            });
            dialog.getContentPane().add(okButton,BorderLayout.SOUTH);
            dialog.getRootPane().setDefaultButton(okButton);
            dialog.getRootPane().registerKeyboardAction(
                    evt -> dialog.dispose(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
            dialog.pack();
            pf.requestFocusInWindow();
            dialog.setVisible(true);
        }
        try
        {
            return connections.getConnection(connection,log);
        }
        catch(SQLException e)
        {
            info.password = oldPassword;
            throw e;
        }
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
