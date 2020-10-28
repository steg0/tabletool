package de.steg0.deskapps.tabletool;

import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * This aims to be a simple tabular grid that has DB connectivity.
 * <p>
 * Start example from packaged JAR with Windows L&amp;F and Oracle as well
 * as DB2 drivers:
 * <p>
 * <code>
 * java -jar tabletool-0.1-SNAPSHOT.jar 
 *      -XX:+UseSerialGC
 *      -Dfile.encoding=Cp1252 
 *      -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel 
 *      -cp $HOME/.m2/repository/com/oracle/ojdbc7/12.1.0.1/ojdbc7-12.1.0.1.jar\;$HOME/.m2/repository/com/ibm/db2/jcc/11.1.4.4/jcc-11.1.4.4.jar\;$APPDATA/tabletool 
 * </code>
 * <p>
 * Note that $APPDATA/tabletool is added to the classpath above so that
 * <code>tabletool.properties</code> might be found there.
 */
public class TableTool
extends WindowAdapter
{

    JFrame frame;
    File workspace;
    TabSetController controller;
    
    TableTool(String workspacefile)
    {
        if(workspacefile!=null) workspace = new File(workspacefile);
    }
    
    void showJdbcBuffer()
    {
        String title = (workspace!=null? workspace.getName()+" - " : "") + 
                "Tabletool";
        frame = new JFrame(title);
        frame.setIconImages(getIcons());
        frame.getContentPane().setLayout(new GridBagLayout());
        var propertyHolder = new PropertyHolder();
        if(ensureFrameDefaults(propertyHolder))
        {
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(this);
            
            controller = new TabSetController(frame,propertyHolder);
            
            if(ensureWorkspace())
            {
                var contentPaneConstraints = new GridBagConstraints();
                contentPaneConstraints.fill = GridBagConstraints.BOTH;
                contentPaneConstraints.anchor = GridBagConstraints.NORTHWEST;
                contentPaneConstraints.weightx = 
                        contentPaneConstraints.weighty = 1;
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
    }
    
    List<Image> getIcons()
    {
        var icon16 = new ImageIcon(getClass().getResource("icon16.png"));
        var icon32 = new ImageIcon(getClass().getResource("icon32.png"));
        var icon = new ImageIcon(getClass().getResource("icon.png"));
        return List.of(icon16.getImage(),icon32.getImage(),icon.getImage());
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
    
    boolean ensureWorkspace()
    {
        if(workspace==null || !workspace.exists()) controller.add(null);
        else try
        {
            controller.restoreWorkspace(workspace);
        }
        catch(IOException e)
        {
            JOptionPane.showMessageDialog(
                    frame,
                    "Error loading workspace: "+e.getMessage(),
                    "Error loading workspace",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    @Override
    public void windowClosing(WindowEvent event)
    {
        if(workspace==null) return;
        try
        {
            controller.saveWorkspace(workspace);
        }
        catch(IOException e)
        {
            JOptionPane.showMessageDialog(
                    frame,
                    "Error saving workspace: "+e.getMessage(),
                    "Error saving workspace",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    public void windowClosed(WindowEvent e)
    {
        System.exit(0);
    }

    public static void main(String[] args)
    {
        String workspacefile=null;
        if(args.length>0) workspacefile=args[0];
        TableTool ttool = new TableTool(workspacefile);
        ttool.showJdbcBuffer();
    }
    
}
