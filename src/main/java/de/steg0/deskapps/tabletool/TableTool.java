package de.steg0.deskapps.tabletool;

import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.sql.SQLException;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
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
            
            JMenuBar menubar = getMenuBar(controller);
            frame.setJMenuBar(menubar);
            
            frame.pack();
            frame.setVisible(true);
        }
    }
    
    JMenuBar getMenuBar(TabSetController controller)
    {
        var menubar = new JMenuBar();
        
        var filemenu = new JMenu("File");
        filemenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem item;

        item = new JMenuItem(controller.addAction);
        item.setMnemonic(KeyEvent.VK_N);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_T,ActionEvent.CTRL_MASK));
        filemenu.add(item);
        
        item = new JMenuItem(controller.loadAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_O,ActionEvent.CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_O);
        filemenu.add(item);

        item = new JMenuItem(controller.saveAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_S,ActionEvent.CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_S);
        filemenu.add(item);

        item = new JMenuItem(controller.closeAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_W,ActionEvent.CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_C);
        filemenu.add(item);

        menubar.add(filemenu);
        
        return menubar;
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
