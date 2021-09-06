package de.steg0.deskapps.tabletool;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * This aims to be a simple tabular grid that has DB connectivity.
 * <p>
 * Consult the README.txt file available in the package to find out
 * about operation and invocation options.
 */
public class Tabtype
extends WindowAdapter
{

    JFrame frame;
    File properties,workspace,sqlFiles[];
    TabSetController controller;
    
    Tabtype(String propertiesfile,String workspacefile)
    {
        if(propertiesfile!=null) properties = new File(propertiesfile);
        if(workspacefile!=null) workspace = new File(workspacefile);
    }
    
    Tabtype(String propertiesfile,String[] sqlFiles)
    {
        if(propertiesfile!=null) properties = new File(propertiesfile);
        this.sqlFiles=Arrays.stream(sqlFiles)
                .map((p) -> new File(p))
                .toArray(File[]::new);
    }
    
    void showJdbcBuffer()
    {
        String title = (workspace!=null? workspace.getName()+" - " : "") + 
                "Tabtype " + ManagementFactory.getRuntimeMXBean().getName();
        frame = new JFrame(title);
        frame.setIconImages(getIcons());
        frame.getContentPane().setLayout(new GridBagLayout());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(this);
        
        var propertyHolder = new PropertyHolder(properties);

        controller = ensureFrameConfiguration(propertyHolder);
            
        ensureWorkspace();
        
        var contentPaneConstraints = new GridBagConstraints();
        contentPaneConstraints.fill = GridBagConstraints.BOTH;
        contentPaneConstraints.anchor = GridBagConstraints.NORTHWEST;
        contentPaneConstraints.weightx = 
                contentPaneConstraints.weighty = 1;
        frame.getContentPane().add(
                controller.tabbedPane,
                contentPaneConstraints
        );
        
        controller.recreateMenuBar();
        
        frame.pack();
        frame.setVisible(true);
    }
    
    static List<Image> getIcons()
    {
        var icon16 = new ImageIcon(Tabtype.class.getResource("icon16.png"));
        var icon32 = new ImageIcon(Tabtype.class.getResource("icon32.png"));
        var icon = new ImageIcon(Tabtype.class.getResource("icon.png"));
        return List.of(icon16.getImage(),icon32.getImage(),icon.getImage());
    }
    
    TabSetController ensureFrameConfiguration(PropertyHolder propertyHolder)
    {
        try
        {
            propertyHolder.load();
            frame.getContentPane().setPreferredSize(
                    propertyHolder.getDefaultFrameSize());
            frame.setLocation(propertyHolder.getDefaultFrameLocation());
            return new TabSetController(frame,propertyHolder);
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(
                    frame,
                    "Error loading configuration: "+e.getMessage(),
                    "Error loading configuration",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        return null;
    }
    
    void ensureWorkspace()
    {
        try
        {
            if(workspace==null)
            {
                if(sqlFiles==null) controller.add(true);
                else for(File f : sqlFiles) controller.load(f);
            }
            else if(!workspace.exists())
            {
                controller.add(true);
            }
            else
            {
                controller.restoreWorkspace(workspace);
            }
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(
                    frame,
                    "Error loading workspace: "+e.getMessage(),
                    "Error loading workspace",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(2);
        }
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
        String propertiesfile=null;
        
        int optind=0;
        ARGS: for(;optind<args.length&&args[optind].startsWith("-");optind++)
        {
            switch(args[optind])
            {
            case "-config":
                propertiesfile = args[++optind];
                break;
            case "--":
                optind++;
                break ARGS;
            }
        }
        
        Tabtype m;
        if(optind<args.length)
        {
            String firstdoc=args[optind];
            if(firstdoc.toLowerCase().endsWith(".xml"))
            {
                m = new Tabtype(propertiesfile,firstdoc);
            }
            else
            {
                m = new Tabtype(propertiesfile,
                        Arrays.copyOfRange(args,optind,args.length));
            }
        }
        else
        {
            m = new Tabtype(propertiesfile,(String)null);
        }
        m.showJdbcBuffer();
    }
    
}
