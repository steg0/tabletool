package de.steg0.deskapps.tabletool;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

public class JdbcNotebookController
{

    ConnectionListModel connections;
    PropertyHolder propertyHolder;
    List<JdbcBufferController> buffers = new ArrayList<>();
    JComboBox<String> connectionsSelector;
    JTextArea log = new JTextArea();
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
        
        var buffer = new JdbcBufferController((t) -> log.setText(t));
        add(buffer);

        var logBufferPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        logBufferPane.setResizeWeight(.85);
        
        var bufferPane = new JScrollPane(bufferPanel);
        bufferPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                buffers.get(0).focusEditor();
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
    
    void add(JdbcBufferController c)
    {
        buffers.add(c);
        var panelConstraints = new GridBagConstraints();
        panelConstraints.anchor = GridBagConstraints.NORTHWEST;
        panelConstraints.weighty = panelConstraints.weightx = 1;
        bufferPanel.add(c.panel,panelConstraints);
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
