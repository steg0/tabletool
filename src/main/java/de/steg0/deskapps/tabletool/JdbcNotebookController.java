package de.steg0.deskapps.tabletool;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
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
import javax.swing.JViewport;

class JdbcNotebookController
{
    JFrame parent;
    PropertyHolder propertyHolder;
    TabSetController.Actions tabSetControllerActions;

    File file;
    
    ConnectionListModel connections;
    JComboBox<String> connectionsSelector;

    JButton commitButton = new JButton("Commit");
    JButton rollbackButton = new JButton("Rollback");
    JButton disconnectButton = new JButton("Disconnect");
    JCheckBox autocommitCb = new JCheckBox("Autocommit",
            Connections.AUTOCOMMIT_DEFAULT);
    
    JScrollPane bufferPane;
    
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
            Connections connections,
            TabSetController.Actions tabSetControllerActions)
    {
        this.propertyHolder = propertyHolder;
        this.connections = new ConnectionListModel(connections);
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
        
        connectionsSelector = new JComboBox<>(this.connections);
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
        bufferPane = new JScrollPane(bufferPanel);
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
        void nextBuffer(JdbcBufferController source);
        void previousBuffer(JdbcBufferController source);
        void scrollRectToVisible(JdbcBufferController source,Rectangle rect);
    }
    
    Actions actions = new Actions()
    {
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
        public void scrollRectToVisible(JdbcBufferController source,
                Rectangle rect)
        {
            Rectangle sourceRect = source.panel.getBounds();
            JViewport viewport = bufferPane.getViewport();
            Point position = viewport.getViewPosition();
            bufferPane.getViewport().scrollRectToVisible(new Rectangle(
                    (int)(sourceRect.getX() + rect.getX() - position.getX()),
                    (int)(sourceRect.getY() + rect.getY() - position.getY()),
                    (int)rect.getWidth(),
                    (int)rect.getHeight()
            ));
        }
    };
    
    void add(JdbcBufferController c)
    {
        c.addEditorFocusListener(new FocusListener()
        {
            @Override 
            public void focusLost(FocusEvent e)
            {
                lastFocusedBuffer = buffers.indexOf(c);
            }
            @Override 
            public void focusGained(FocusEvent e) { }
        });

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
    
    public void store()
    {
        if(file==null)
        {
            var filechooser = new JFileChooser();
            int returnVal = filechooser.showSaveDialog(bufferPanel);
            if(returnVal != JFileChooser.APPROVE_OPTION) return;
            file=filechooser.getSelectedFile();
            tabSetControllerActions.setTabTitleFor(file);
        }
        try(Writer w = new FileWriter(file))
        {
            store(w);
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

    /**blocking */
    void store(Writer w)
    throws IOException
    {
        for(JdbcBufferController buffer : buffers)
        {
            buffer.store(w);
        }
    }
    
    /**blocking */
    void load(LineNumberReader r)
    throws IOException
    {
        assert buffers.size()==1 : "load only supports uninitialized panels";
        String nextline=buffers.get(0).load(r);
        while(nextline != null)
        {
            var newBufferController = new JdbcBufferController(
                    parent,logConsumer,actions);
            newBufferController.appendText(nextline);
            nextline = newBufferController.load(r);
            add(newBufferController);
        }
        bufferPanel.revalidate();
        buffers.get(0).focusEditor();
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
