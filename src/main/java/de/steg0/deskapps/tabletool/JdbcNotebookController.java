package de.steg0.deskapps.tabletool;

import java.sql.Connection;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class JdbcNotebookController
{

    Connection connection;
    JPanel panel = new JPanel();
    JScrollPane pane = new JScrollPane(panel);
    
    JdbcNotebookController(Connection connection)
    {
        this.connection = connection;
        
        JdbcBufferController buffer = new JdbcBufferController(connection);
        add(buffer);
    }
    
    void add(JdbcBufferController c)
    {
        panel.add(c.panel);
    }
    
}
