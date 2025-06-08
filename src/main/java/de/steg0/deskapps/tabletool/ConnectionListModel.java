package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.function.Consumer;

import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.steg0.deskapps.tabletool.PropertyHolder.ConnectionInfo;

/**
 * The model to make connections, and a specific selected index in the list,
 * available to {@link NotebookController}. It can also show a password
 * dialog if a connection is configured for prompted passwords.
 */
class ConnectionListModel
implements ComboBoxModel<Connections.ConnectionState>,
        TableModel
{
    private static final String PROMPT = "PROMPT";

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

    static class PasswordPromptCanceledException extends Exception
    {
    }

    /**
     * blocking; establishes connection if needed. Note: this checks if a
     * password has the special value "PROMPT", in which case it asks
     * the user for a password and sets it in the connection info. This
     * remains there until the next property refresh.
     */
    ConnectionWorker getConnection(Connections.ConnectionState connection,
            Consumer<String> log,JFrame parent)
    throws SQLException,PasswordPromptCanceledException
    {
        ConnectionInfo info = connection.info();
        String oldPassword = info.password;
        if(PROMPT.equals(info.password) &&
           !connections.isConnected(connection))
        {
            promptForPassword(parent,info);

            if(PROMPT.equals(info.password))
            {
                throw new PasswordPromptCanceledException();
            }
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
    
    private void promptForPassword(JFrame parent,ConnectionInfo info)
    {
        var dialog = new JDialog(parent,"Credentials prompt",true);
        var inputPanel = new JPanel(new GridBagLayout());
        inputPanel.add(new JLabel("Connection: "+info.name),
                new GridBagConstraints(0,0,2,1,0,0,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.NONE,
                        new Insets(0,5,0,5),5,5));
        inputPanel.add(new JLabel("User: "+info.username),
                new GridBagConstraints(0,1,2,1,0,0,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.NONE,
                        new Insets(0,5,0,5),5,5));
        inputPanel.add(new JLabel("Password:"),
                new GridBagConstraints(0,2,1,1,0,0,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.NONE,
                        new Insets(0,5,0,0),5,5));
        var pf = new JPasswordField();
        pf.setPreferredSize(new Dimension(120,20));
        inputPanel.add(pf,
                new GridBagConstraints(1,2,1,1,1,0,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.HORIZONTAL,
                        new Insets(0,0,0,5),5,5));
        dialog.getContentPane().add(inputPanel);

        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        ((BorderLayout)dialog.getContentPane().getLayout()).setVgap(5);
        
        var okButton = new JButton("OK");
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

    @Override
    public int getRowCount()
    {
        return getSize();
    }

    @Override
    public int getColumnCount()
    {
        return 4;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        return switch(columnIndex)
        {
            case 1 -> "Url";
            case 2 -> "User";
            case 3 -> "Prompt for password?";
            default -> "Name";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return switch(columnIndex)
        {
            case 1 -> String.class;
            case 2 -> String.class;
            case 3 -> Boolean.class;
            default -> String.class;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex,int columnIndex)
    {
        return columnIndex > 0;
    }

    @Override
    public Object getValueAt(int rowIndex,int columnIndex)
    {
        Connections.ConnectionState state = getElementAt(rowIndex);
        ConnectionInfo info = state.info();
        return switch(columnIndex)
        {
            case 1 -> info.url;
            case 2 -> info.username;
            case 3 -> PROMPT.equals(info.password);
            default -> state.toString();
        };
    }

    @Override
    public void setValueAt(Object value,int rowIndex,int columnIndex)
    {
        Connections.ConnectionState state = getElementAt(rowIndex);
        ConnectionInfo info = state.info();
        switch(columnIndex)
        {
            case 1:
                info.url = value == null? "jdbc:" : value.toString();
                break;
            case 2:
                info.username = value == null? "" : value.toString();
                break;
            case 3:
                if(Boolean.TRUE.equals(value)) info.password=PROMPT;
        }
    }

    @Override
    public void addTableModelListener(TableModelListener l)
    {
    }

    @Override
    public void removeTableModelListener(TableModelListener l)
    {
    }
}
