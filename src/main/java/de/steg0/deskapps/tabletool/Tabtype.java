package de.steg0.deskapps.tabletool;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.LogManager;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * This aims to be a simple tabular grid that has DB connectivity.
 * <p>
 * Consult the README.txt file available in the package to find out
 * about operation and invocation options.
 */
public class Tabtype
extends WindowAdapter
{

    private JFrame frame,cellDisplay=new JFrame(),infoDisplay=new JFrame();
    private File properties[],workspace,sqlFiles[];
    private TabSetController controller;
    
    private Tabtype(Collection<String> propertiesFiles,String workspaceFile)
    {
        properties = propertiesFiles.stream().map(f -> new File(f))
                .toArray(File[]::new);
        if(workspaceFile!=null) workspace = new File(workspaceFile);
    }
    
    private Tabtype(Collection<String> propertiesFiles,String[] sqlFiles)
    {
        properties = propertiesFiles.stream().map(f -> new File(f))
                .toArray(File[]::new);
        this.sqlFiles=Arrays.stream(sqlFiles)
                .map((p) -> new File(p))
                .toArray(File[]::new);
    }
    
    private void showBuffer()
    {
        String wsprefix = (workspace!=null? workspace.getName().replaceFirst(
                "(.)\\.[^.]+$","$1")+" - " : "");
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        String title = wsprefix + "Tabtype " + jvmName;
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
        
        controller.menubar.recreate();
        
        frame.pack();
        Point location = propertyHolder.getDefaultFrameLocation();
        if(location==null) frame.setLocationRelativeTo(null);
        else frame.setLocation(location);

        Point parentLocation = frame.getLocation();
        int dialogx = (int)parentLocation.getX()+30,
            dialogy = (int)parentLocation.getY()+30;
        cellDisplay.setLocation(dialogx,dialogy);
        infoDisplay.setLocation(dialogx,dialogy);
        infoDisplay.getContentPane().setPreferredSize(
                propertyHolder.getDefaultFrameSize());

        frame.setVisible(true);
    }
    
    static List<Image> getIcons()
    {
        var icon16 = new ImageIcon(Tabtype.class.getResource("icon16.png"));
        var icon32 = new ImageIcon(Tabtype.class.getResource("icon32.png"));
        var icon = new ImageIcon(Tabtype.class.getResource("icon.png"));
        return List.of(icon16.getImage(),icon32.getImage(),icon.getImage());
    }
    
    private TabSetController ensureFrameConfiguration(
            PropertyHolder propertyHolder)
    {
        try
        {
            propertyHolder.load();
            frame.getContentPane().setPreferredSize(
                    propertyHolder.getDefaultFrameSize());
            Color frameBackground = propertyHolder.getFrameBackground();
            if(frameBackground!=null) frame.getContentPane().setBackground(
                    frameBackground);
            UIManager.getDefaults().putDefaults(propertyHolder
                    .getColorUIDefaults());
            UIManager.getDefaults().putDefaults(propertyHolder
                    .getGradientUIDefaults());
            return new TabSetController(frame,cellDisplay,infoDisplay,
                    propertyHolder,workspace);
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
    
    private void ensureWorkspace()
    {
        try
        {
            if(workspace==null)
            {
                if(sqlFiles==null) controller.add(-1);
                else for(File f : sqlFiles) controller.load(f);
            }
            else if(!workspace.exists() || workspace.length()==0)
            {
                controller.add(-1);
            }
            else
            {
                controller.restoreWorkspace();
            }
        }
        catch(Exception e)
        {
            Object[]
                    desktopChoices={"Close","Retry","Edit workspace file"},
                    choices={"Close","Retry"};
            int choice=JOptionPane.showOptionDialog(
                    frame,
                    "Error loading workspace: "+e.getMessage(),
                    "Error loading workspace",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.ERROR_MESSAGE,
                    null,
                    Desktop.isDesktopSupported()? desktopChoices : choices,
                    "Close"
            );
            if(choice==1)
            {
                ensureWorkspace();
                return;
            }
            else if(choice==2) try
            {
                Desktop.getDesktop().edit(workspace);
                choice=JOptionPane.showOptionDialog(
                        frame,
                        "The workspace file has been opened for editing.",
                        "Workspace file opened",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        choices,
                        "Close"
                );
                if(choice==1)
                {
                    ensureWorkspace();
                    return;
                }
            }
            catch(IOException e0)
            {
                JOptionPane.showMessageDialog(
                        frame,
                        "Error editing workspace file: "+e0.getMessage(),
                        "Error editing workspace file",
                        JOptionPane.ERROR_MESSAGE);
            }
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
                    "Unsaved notebooks exist. Exit?",
                    "Unsaved notebook warning",
                    JOptionPane.YES_NO_OPTION);
            if(option!=JOptionPane.YES_OPTION)
            {
                return;
            }
        }

        frame.dispose();
        
        try
        {
            controller.saveWorkspace();
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
        Collection<String> propertiesfiles=new ArrayList<>();
        
        int optind=0;
        ARGS: for(;optind<args.length&&args[optind].startsWith("-");optind++)
        {
            switch(args[optind])
            {
            case "-config":
                propertiesfiles.add(args[++optind]);
                break;
            case "-logconfig":
                File f = new File(args[++optind]);
                if(f.exists()) try(var fs = new FileInputStream(f))
                {
                    LogManager.getLogManager().readConfiguration(fs);
                }
                catch(IOException e)
                {
                    JOptionPane.showMessageDialog(
                            null,
                            "Error loading logger configuration: "+e.getMessage(),
                            "Error loading logger configuration",
                            JOptionPane.ERROR_MESSAGE);
                }
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
            if(firstdoc.toLowerCase().endsWith(".xml") || 
               firstdoc.toLowerCase().endsWith(".tabtype"))
            {
                m = new Tabtype(propertiesfiles,firstdoc);
            }
            else
            {
                m = new Tabtype(propertiesfiles,
                        Arrays.copyOfRange(args,optind,args.length));
            }
        }
        else
        {
            m = new Tabtype(propertiesfiles,(String)null);
        }
        m.showBuffer();
    }
    
}
