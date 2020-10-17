package de.steg0.deskapps.tabletool;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

    JPanel panel = new JPanel(new GridBagLayout());
    Connection connection;
    JTextArea editor = new JTextArea();
    
    JdbcBufferController(Connection connection)
    {
        this.connection=connection;

        var editorConstraints = new GridBagConstraints();
        editorConstraints.anchor = GridBagConstraints.WEST;
        editorConstraints.gridy = 0;
        panel.add(editor,editorConstraints);
        
        JButton executeButton = new JButton("Go");
        var buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridy = 1;
        panel.add(executeButton,buttonConstraints);
        
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
                    var resultviewConstraints = new GridBagConstraints();
                    resultviewConstraints.anchor = GridBagConstraints.WEST;
                    resultviewConstraints.gridy = 2;
                    panel.add(resultview,resultviewConstraints);
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
