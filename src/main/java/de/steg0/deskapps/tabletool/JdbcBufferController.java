package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;

public class JdbcBufferController
{

    JPanel panel = new JPanel(new BorderLayout());
    Connection connection;
    JTextArea editor = new JTextArea();
    
    JdbcBufferController(Connection connection)
    {
        this.connection=connection;
        
        panel.add(editor,BorderLayout.NORTH);
        JButton executeButton = new JButton("Go");
        panel.add(executeButton,BorderLayout.CENTER);
        executeButton.addActionListener((ActionEvent e) ->
        { 
            try
            {
                fetch();
            }
            catch(SQLException e0)
            {
                // TODO
                e0.printStackTrace();
            }
        });
    }
    
    ResultSet fetch()
    throws SQLException
    {
        String text = editor.getText();
        try(Statement st = connection.createStatement())
        {
            if(st.execute(text))
            {
                try(ResultSet rs = st.getResultSet())
                {
                    ResultSetTableModel rsm = new ResultSetTableModel();
                    rsm.update(rs);
                    JTable resultview = new JTable(rsm);
                    if(panel.getComponentCount()==3) panel.remove(2);
                    panel.add(resultview,BorderLayout.SOUTH);
                    panel.revalidate();
                }
            }
            else
            {
                // TODO
                System.out.printf("%d row(s) affected\n",st.getUpdateCount());
            }
        }
        return null;
    }
}
