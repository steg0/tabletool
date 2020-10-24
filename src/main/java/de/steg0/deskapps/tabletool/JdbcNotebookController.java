package de.steg0.deskapps.tabletool;

import java.awt.Color;
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
import java.util.function.Consumer;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

class JdbcNotebookController
{

    ConnectionListModel connections;
    PropertyHolder propertyHolder;
    List<JdbcBufferController> buffers = new ArrayList<>();
    JComboBox<String> connectionsSelector;
    JTextArea log = new JTextArea();
    Consumer<String> logConsumer = (t) -> log.setText(t);
    JPanel bufferPanel = new JPanel(new GridBagLayout());
    JPanel notebookPanel = new JPanel(new GridBagLayout());
    
    {
        log.setEditable(false);
    }
    
    JdbcNotebookController(PropertyHolder propertyHolder)
    {
        this.propertyHolder = propertyHolder;
        connections = new ConnectionListModel(propertyHolder);
    
        var connectionPanel = new JPanel();
        connectionsSelector = new JComboBox<>(connections);
        connectionsSelector.addItemListener((e) -> updateConnection(e));
        connectionPanel.add(connectionsSelector);
        var connectionPanelConstraints = new GridBagConstraints();
        connectionPanelConstraints.anchor = GridBagConstraints.EAST;
        notebookPanel.add(connectionPanel,connectionPanelConstraints);
        
        var buffer = new JdbcBufferController(logConsumer,actions);
        add(buffer);

        var logBufferPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        logBufferPane.setResizeWeight(.85);
        
        var bufferPane = new JScrollPane(bufferPanel);
        bufferPane.addMouseListener(new MouseAdapter() {
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
                        break;
                    }
                }
                buffers.get(buffers.size()-1).focusEditor();
            }
        });
        logBufferPane.add(bufferPane);
        var logPane = new JScrollPane(log);
        logBufferPane.add(logPane);
        
        var bufferPaneConstraints = new GridBagConstraints();
        bufferPaneConstraints.fill = GridBagConstraints.BOTH;
        bufferPaneConstraints.weighty = bufferPaneConstraints.weightx = 1;
        bufferPaneConstraints.gridy = 1;
        notebookPanel.add(logBufferPane,bufferPaneConstraints);
    }
    
    interface Actions
    {
        void nextBuffer(JdbcBufferController source);
        void previousBuffer(JdbcBufferController source);
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
                        var newBufferController =
                                new JdbcBufferController(logConsumer,actions);
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
    void updateConnection(ItemEvent event)
    {
        try
        {
            for(JdbcBufferController buffer : buffers)
            {
                buffer.connection = connections.getConnection(event.getItem());
            }
            buffers.get(0).focusEditor();
        }
        catch(SQLException e)
        {
            log.setText("Error retrieving connection at "+new Date()+"\n");
            for(;e!=null;e=e.getNextException())
            {
                log.append("Error code: "+e.getErrorCode()+"\n");
                log.append("SQL State: "+e.getSQLState()+"\n");
                log.append(e.getMessage()+"\n");
            }
        }
    }
    
}
