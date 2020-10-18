package de.steg0.deskapps.tabletool;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * The steg0 Tabletool.
 * 
 * <p>
 * This aims to be a simple tabular grid that accepts LISP expressions in its
 * cells, can edit CSV fast and has DB connectivity.
 * </p>
 * 
 * <p>It should also offer table-diff functionality.</p>
 * 
 * Start example:
 * <code>
 * java -cp $HOME/.m2/repository/com/oracle/ojdbc7/12.1.0.1/ojdbc7-12.1.0.1.jar\;target/classes de.steg0.deskapps.tabletool.TableTool 
 * </code>
 */
public class TableTool 
{

    private JFrame frame;
    
    public void showJdbcBuffer()
    throws SQLException
    {
        frame = new JFrame("Tabletool");
        frame.getContentPane().setLayout(new GridBagLayout());
        var propertyHolder = new PropertyHolder();
        if(ensureFrameDefaults(propertyHolder))
        {
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            
            var controller = new JdbcNotebookController(propertyHolder);
    
            var contentPaneConstraints = new GridBagConstraints();
            contentPaneConstraints.fill = GridBagConstraints.BOTH;
            contentPaneConstraints.anchor = GridBagConstraints.NORTHWEST;
            contentPaneConstraints.weightx = contentPaneConstraints.weighty = 1;
            frame.getContentPane().add(
                    controller.notebookPanel,
                    contentPaneConstraints
            );
            frame.pack();
            frame.setVisible(true);
        }
    }
    
    boolean ensureFrameDefaults(PropertyHolder propertyHolder)
    {
        try
        {
            propertyHolder.load();
            frame.getContentPane().setPreferredSize(
                    propertyHolder.getDefaultFrameSize());
            frame.setLocation(propertyHolder.getDefaultFrameLocation());
            return true;
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(
                    frame,
                    "Error loading properties: "+e.getMessage(),
                    "Error loading properties",
                    JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }
    
    public static void main(String[] args)
    throws SQLException
    {
        TableTool ttool = new TableTool();
        ttool.showJdbcBuffer();
    }
    
}
