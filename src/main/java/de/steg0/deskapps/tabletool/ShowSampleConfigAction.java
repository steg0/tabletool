package de.steg0.deskapps.tabletool;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

class ShowSampleConfigAction extends AbstractAction
{
    JFrame parent;
    
    ShowSampleConfigAction(JFrame parent)
    {
        super("Show Sample Config");
        this.parent = parent;
    }
    
    @Override
    public void actionPerformed(ActionEvent event)
    {
        try(var i=getClass().getResourceAsStream("/sample.properties.xml");
                var e=new DesktopExporter("TabtypeSampleConfiguration",".xml"))
        {
            i.transferTo(e.getOutputStream());
            e.openWithDesktop();
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(
                    parent,
                    "Error loading sample configuration file: "+e.getMessage(),
                    "Error loading sample configuration file",
                    JOptionPane.ERROR_MESSAGE);
        }
    }  
}