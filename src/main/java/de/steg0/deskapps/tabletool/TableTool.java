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
 * Start example &ndash; Windows L&amp;F:
 * <code>
 * java -XX:+UseSerialGC -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel -cp $HOME/.m2/repository/com/oracle/ojdbc7/12.1.0.1/ojdbc7-12.1.0.1.jar\;target/classes de.steg0.deskapps.tabletool.TableTool 
 * </code>
 * Metal L&amp;F with Steel theme:
 * <code>
 * java -XX:+UseSerialGC -Dswing.metalTheme=steel -cp $HOME/.m2/repository/com/oracle/ojdbc7/12.1.0.1/ojdbc7-12.1.0.1.jar\;target/classes de.steg0.deskapps.tabletool.TableTool 
 * </code>
 */
public class TableTool 
{

    JFrame frame;
    
    void showJdbcBuffer()
    throws SQLException
    {
        frame = new JFrame("Tabletool");
        frame.getContentPane().setLayout(new GridBagLayout());
        var propertyHolder = new PropertyHolder();
        if(ensureFrameDefaults(propertyHolder))
        {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            var controller = new TabSetController(frame,propertyHolder);
    
            var contentPaneConstraints = new GridBagConstraints();
            contentPaneConstraints.fill = GridBagConstraints.BOTH;
            contentPaneConstraints.anchor = GridBagConstraints.NORTHWEST;
            contentPaneConstraints.weightx = contentPaneConstraints.weighty = 1;
            frame.getContentPane().add(
                    controller.tabbedPane,
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
