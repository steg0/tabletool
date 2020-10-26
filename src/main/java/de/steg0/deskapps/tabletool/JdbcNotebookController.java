package de.steg0.deskapps.tabletool;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

class JdbcNotebookController
{
    JFrame parent;
    PropertyHolder propertyHolder;
    TabSetController.Actions tabSetControllerActions;

    ConnectionListModel connections;
    JComboBox<String> connectionsSelector;

    JButton commitButton = new JButton("Commit");
    JButton rollbackButton = new JButton("Rollback");
    JButton disconnectButton = new JButton("Disconnect");
    JCheckBox autocommitCb = new JCheckBox("Autocommit",
            ConnectionListModel.AUTOCOMMIT_DEFAULT);
    
    JTextArea log = new JTextArea();
    Consumer<String> logConsumer = (t) -> log.setText(t);
    
    {
        log.setEditable(false);
    }
    
    List<JdbcBufferController> buffers = new ArrayList<>();
    int lastFocusedBuffer;
    JPanel bufferPanel = new JPanel(new GridBagLayout());
    JPanel notebookPanel = new JPanel(new GridBagLayout());
    
    JdbcNotebookController(
            JFrame parent,
            PropertyHolder propertyHolder,
            ConnectionListModel connections,
            TabSetController.Actions tabSetControllerActions)
    {
        this.propertyHolder = propertyHolder;
        this.connections = connections;
        this.tabSetControllerActions = tabSetControllerActions;
        
        var connectionPanel = new JPanel();
        
        commitButton.addActionListener((e) -> 
        {
            onConnection((c) -> c.commit(logConsumer));
        });
        connectionPanel.add(commitButton);
        
        rollbackButton.addActionListener((e) -> 
        {
            onConnection((c) -> c.rollback(logConsumer));
        });
        connectionPanel.add(rollbackButton);
        
        disconnectButton.addActionListener((e) ->
        {
            onConnection((c) -> 
            {
                c.disconnect(logConsumer);
                tabSetControllerActions.reportDisconnect(c);
            });
        });
        connectionPanel.add(disconnectButton);
        
        autocommitCb.addActionListener((e) -> 
        {
            onConnection((c) -> c.setAutoCommit(
                    autocommitCb.isSelected(),logConsumer));
        });
        connectionPanel.add(autocommitCb);
        
        connectionsSelector = new JComboBox<>(connections);
        connectionsSelector.addItemListener((e) -> updateConnection(e));
        connectionPanel.add(connectionsSelector);
        
        var connectionPanelConstraints = new GridBagConstraints();
        connectionPanelConstraints.anchor = GridBagConstraints.EAST;
        
        notebookPanel.add(connectionPanel,connectionPanelConstraints);
        
        var buffer = new JdbcBufferController(parent,logConsumer,actions);
        add(buffer);

        var logBufferPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        logBufferPane.setResizeWeight(.85);
        
        bufferPanel.setBackground(buffer.editor.getBackground());
        var bufferPane = new JScrollPane(bufferPanel);
        bufferPane.addMouseListener(new BufferPaneMouseListener());
        logBufferPane.add(bufferPane);
        var logPane = new JScrollPane(log);
        logBufferPane.add(logPane);
        
        var bufferPaneConstraints = new GridBagConstraints();
        bufferPaneConstraints.fill = GridBagConstraints.BOTH;
        bufferPaneConstraints.weighty = bufferPaneConstraints.weightx = 1;
        bufferPaneConstraints.gridy = 1;
        notebookPanel.add(logBufferPane,bufferPaneConstraints);
    }
    
    class BufferPaneMouseListener extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            Point reference = new Point(0,e.getY());
            var component = bufferPanel.getComponentAt(reference);
            for(var buffer : buffers)
            {
                if(buffer.panel == component)
                {
                    buffer.focusEditor();
                    return;
                }
            }
            buffers.get(buffers.size()-1).focusEditor();
        }
    }
    
    interface Actions
    {
        void bufferFocusLost(JdbcBufferController source);
        void nextBuffer(JdbcBufferController source);
        void previousBuffer(JdbcBufferController source);
        void newTab();
        void removeTab();
        void store();
    }
    
    Actions actions = new Actions()
    {
        @Override
        public void bufferFocusLost(JdbcBufferController source)
        {
            lastFocusedBuffer = buffers.indexOf(source);
        }
        
        @Override
        public void nextBuffer(JdbcBufferController source)
        {
            for(int i=0;i<buffers.size();i++)
            {
                if(buffers.get(i) == source)
                {
                    if(buffers.size() <= i+1)
                    {
                        var newBufferController = new JdbcBufferController(
                                parent,logConsumer,actions);
                        newBufferController.connection =
                                buffers.get(i).connection;
                        add(newBufferController);
                    }
                    bufferPanel.revalidate();
                    buffers.get(i+1).focusEditor();
                    break;
                }
            }
        }

        @Override
        public void previousBuffer(JdbcBufferController source)
        {
            for(int i=0;i<buffers.size();i++)
            {
                if(buffers.get(i) == source && i > 0)
                {
                    buffers.get(i-1).focusEditor();
                    break;
                }
            }
        }

        @Override
        public void newTab()
        {
            tabSetControllerActions.add();
        }

        @Override
        public void removeTab()
        {
            tabSetControllerActions.removeSelected();
        }

        @Override
        public void store()
        {
            var filechooser = new JFileChooser();
            int returnVal = filechooser.showSaveDialog(bufferPanel);
            if (returnVal == JFileChooser.APPROVE_OPTION) 
            {
                try(Writer w = new FileWriter(filechooser.getSelectedFile()))
                {
                    JdbcNotebookController.this.store(w);
                }
                catch(IOException e)
                {
                    JOptionPane.showMessageDialog(
                            bufferPanel,
                            "Error saving: "+e.getMessage(),
                            "Error saving",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    };
    
    void add(JdbcBufferController c)
    {
        bufferPanel.removeAll();
        for(int i=0;i<buffers.size();i++)
        {
            var panelConstraints = new GridBagConstraints();
            panelConstraints.anchor = GridBagConstraints.WEST;
            panelConstraints.weightx = 1;
            panelConstraints.gridy=i;
            bufferPanel.add(buffers.get(i).panel,panelConstraints);
        }
        var panelConstraints = new GridBagConstraints();
        panelConstraints.anchor = GridBagConstraints.NORTHWEST;
        panelConstraints.weighty = panelConstraints.weightx = 1;
        panelConstraints.gridy=buffers.size();
        bufferPanel.add(c.panel,panelConstraints);
        buffers.add(c);
    }
    
    /**blocking */
    void store(Writer w)
    throws IOException
    {
        for(JdbcBufferController buffer : buffers)
        {
            buffer.store(w);
        }
    }
    
    void restoreFocus()
    {
        buffers.get(lastFocusedBuffer).focusEditor();
    }
    
    void onConnection(Consumer<ConnectionWorker> c)
    {
        ConnectionWorker selectedConnection = connections.selected();
        if(selectedConnection != null)
        {
            c.accept(selectedConnection);
        }
        else
        {
            logConsumer.accept("No connection available at "+new Date());
        };
    }
    
    /**blocking */
    void updateConnection(ItemEvent event)
    {
        try
        {
            var connection = connections.getConnection(event.getItem());
            /* 
             * If the update was due to a Disconnect action, the connection
             * is closed but not yet cleaned up.
             */
            if(connection.isClosed()) return;
            connection.setAutoCommit(autocommitCb.isSelected(),logConsumer);
            for(JdbcBufferController buffer : buffers)
            {
                buffer.connection = connection;
            }
            restoreFocus();
        }
        catch(SQLException e)
        {
            logConsumer.accept(SQLExceptionPrinter.toString(e));
        }
    }
    
    void reportDisconnect(ConnectionWorker connection)
    {
        for(JdbcBufferController buffer : buffers)
        {
            if(buffer.connection == connection) buffer.connection = null;
        }
        connectionsSelector.setSelectedIndex(-1);
        connectionsSelector.repaint();
    }
    
}
