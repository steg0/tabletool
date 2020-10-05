package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.swing.JFrame;

/**
 * The steg0 Tabletool.
 * 
 * <p>
 * This aims to be a simple tabular grid that accepts LISP expressions in its
 * cells. I. e. like Excel but without Basic.
 * </p>
 * 
 * <p>It should also offer table-diff functionality.</p>
 * 
 * <p>
 * I plan to add database connectivity later on, if I have a need (which I think
 * I'll have) and the necessary spare time.
 * </p>
 */
public class TableTool 
{

    private JFrame frame;
    
    public void showJdbcBuffer(String connectionString,String user,String pw)
    throws SQLException
    {
        frame = new JFrame("Tabletool");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        Connection connection = DriverManager.getConnection(
                connectionString,user,pw);
        
        var bufferController = new JdbcBufferController(connection);

        frame.getContentPane().add(bufferController.panel,
                BorderLayout.CENTER);
        
        frame.pack();
        frame.setVisible(true);
    }
    
    public static void main(String[] args)
    throws SQLException
    {
        TableTool ttool = new TableTool();
        ttool.showJdbcBuffer(args[0],args[1],args[2]);
    }
    
}
