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
 * Consult the README.txt file available in the package to find out
 * about operation and invocation options.
 */
public class Myriad
extends WindowAdapter
{

    JFrame frame;
    File properties,workspace;
    TabSetController controller;
    
    Myriad(String propertiesfile,String workspacefile)
    {
        if(propertiesfile!=null) properties = new File(propertiesfile);
        if(workspacefile!=null) workspace = new File(workspacefile);
    }
    
    void showJdbcBuffer()
    {
        String title = (workspace!=null? workspace.getName()+" - " : "") + 
                "Myriad";
        frame = new JFrame(title);
        frame.setIconImages(getIcons());
        frame.getContentPane().setLayout(new GridBagLayout());
        var propertyHolder = new PropertyHolder(properties);
        if(ensureFrameDefaults(propertyHolder))
        {
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
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
        
        JMenu menu;
        
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        JMenuItem item;

        item = new JMenuItem(controller.addAction);
        item.setMnemonic(KeyEvent.VK_N);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_T,ActionEvent.CTRL_MASK));
        menu.add(item);
        
        item = new JMenuItem(controller.loadAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_O,ActionEvent.CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_O);
        menu.add(item);

        item = new JMenuItem(controller.saveAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_S,ActionEvent.CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_S);
        menu.add(item);

        item = new JMenuItem(controller.closeAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_W,ActionEvent.CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_C);
        menu.add(item);

        menubar.add(menu);
        
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        
        item = new JMenuItem(new HelpAction(frame));
        item.setMnemonic(KeyEvent.VK_R);
        menu.add(item);
        
        menubar.add(menu);
        
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
        if(workspace==null || !workspace.exists()) controller.add(true);
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
        if(controller.isUnsaved())
        {
            int option = JOptionPane.showConfirmDialog(
                    frame,
                    "Unsaved buffers exist. Exit?",
                    "Unsaved buffer warning",
                    JOptionPane.YES_NO_OPTION);
            if(option!=JOptionPane.YES_OPTION)
            {
                return;
            }
        }

        frame.dispose();
        
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
        String workspacefile=null,propertiesfile=null;
        
        int optind=0;
        for(;optind<args.length&&args[optind].startsWith("-");optind++)
        {
            switch(args[optind])
            {
            case "-config":
                propertiesfile = args[++optind];
            case "--":
                break;
            }
        }
        
        if(optind<args.length) workspacefile=args[optind];
        Myriad ttool = new Myriad(propertiesfile,workspacefile);
        ttool.showJdbcBuffer();
    }
    
}
