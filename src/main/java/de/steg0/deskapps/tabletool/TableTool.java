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
        this.frame = new JFrame("Tabletool");
        this.frame.getContentPane().setLayout(new BorderLayout());
        this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        Connection connection = DriverManager.getConnection(
                connectionString,user,pw);
        
        var bufferController = new JdbcBufferController(connection);

        /* 
         * Maybe introduce tabbed wb's someday that can be dragged between
         * frames, like Firefox
         */
        this.frame.getContentPane().add(bufferController.panel,
                BorderLayout.CENTER);
        
        this.frame.pack();
        this.frame.setVisible(true);
    }
    
    public static void main(String[] args)
    throws SQLException
    {
        final TableTool ttool = new TableTool();
        ttool.showJdbcBuffer(args[0],args[1],args[2]);
    }
    
}
