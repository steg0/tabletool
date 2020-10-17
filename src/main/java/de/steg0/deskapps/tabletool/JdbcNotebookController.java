package de.steg0.deskapps.tabletool;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.Connection;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class JdbcNotebookController
{

    Connection connection;
    JPanel panel = new JPanel(new GridBagLayout());
    JScrollPane pane = new JScrollPane(panel);
    
    JdbcNotebookController(Connection connection)
    {
        this.connection = connection;
        
        JdbcBufferController buffer = new JdbcBufferController(connection);
        add(buffer);
    }
    
    void add(JdbcBufferController c)
    {
        var panelConstraints = new GridBagConstraints();
        panelConstraints.anchor = GridBagConstraints.NORTHWEST;
        panelConstraints.weighty = panelConstraints.weightx = 1;
        panel.add(c.panel,panelConstraints);
    }
    
}
