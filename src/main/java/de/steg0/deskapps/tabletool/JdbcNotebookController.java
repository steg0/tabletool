package de.steg0.deskapps.tabletool;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

class JdbcNotebookController
{
    static final boolean AUTOCOMMIT_DEFAULT=false;
    
    JFrame parent;
    PropertyHolder propertyHolder;
    TabSetController.Actions tabSetControllerActions;

    ConnectionListModel connections;
    Executor executor = Executors.newCachedThreadPool();
    JComboBox<String> connectionsSelector;

    JButton commitButton = new JButton("Commit");
    JButton rollbackButton = new JButton("Rollback");
    JCheckBox autocommitCb = new JCheckBox("Autocommit",AUTOCOMMIT_DEFAULT);
    
    JTextArea log = new JTextArea();
    Consumer<String> logConsumer = (t) -> log.setText(t);
    
    {
        log.setEditable(false);
    }
    
    List<JdbcBufferController> buffers = new ArrayList<>();
    int lastFocusedBuffer;
    JPanel bufferPanel = new JPanel(new GridBagLayout());
    JPanel notebookPanel = new JPanel(new GridBagLayout());
    
    JdbcNotebookController(JFrame parent,PropertyHolder propertyHolder,
            TabSetController.Actions tabSetControllerActions)
    {
        this.propertyHolder = propertyHolder;
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
        
        autocommitCb.addActionListener((e) -> 
        {
            onConnection((c) -> c.setAutoCommit(
                    autocommitCb.isSelected(),logConsumer));
        });
        connectionPanel.add(autocommitCb);
        
        connections = new ConnectionListModel(propertyHolder,executor,
                AUTOCOMMIT_DEFAULT);
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
    
}
