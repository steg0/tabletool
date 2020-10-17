package de.steg0.deskapps.tabletool;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
        
        editor.addKeyListener(new KeyListener()
        {
            @Override public void keyReleased(KeyEvent e)
            {
                switch(e.getKeyCode())
                {
                case KeyEvent.VK_ENTER:
                    if(e.isControlDown()) try
                    {
                        fetch();
                    }
                    catch(SQLException e0)
                    {
                        // TODO
                        e0.printStackTrace();
                    }
                }
                
            }
            @Override public void keyTyped(KeyEvent e) { }
            @Override public void keyPressed(KeyEvent e) { }
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
                    if(panel.getComponentCount()==2) panel.remove(1);
                    var resultviewConstraints = new GridBagConstraints();
                    resultviewConstraints.anchor = GridBagConstraints.WEST;
                    resultviewConstraints.gridy = 1;
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
