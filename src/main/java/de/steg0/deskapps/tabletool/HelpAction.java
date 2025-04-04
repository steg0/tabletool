package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

class HelpAction extends AbstractAction
{
    private JFrame parent;
    
    HelpAction(JFrame parent)
    {
        super("README");
        this.parent = parent;
    }
    
    @Override
    public void actionPerformed(ActionEvent event)
    {
        try(var r=new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/README.txt"),
                StandardCharsets.UTF_8)))
        {
            var version = getClass().getPackage().getImplementationVersion();
            if(version==null) version="Development";
            else version="V"+version;
            var dialog = new JDialog(parent,"README - Tabtype "+version,
                    false);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            
            var panel = new JPanel(new BorderLayout());
            
            panel.setBorder(new EmptyBorder(10,10,10,10));
            
            var textarea = new JTextArea(30,77);
            textarea.setEditable(false);
            textarea.setLineWrap(true);
            textarea.setWrapStyleWord(true);
            
            String line;
            while((line=r.readLine())!=null)
            {
                textarea.append(line);
                textarea.append("\n");
            }
            textarea.setCaretPosition(0);
                
            var scrollpane = new JScrollPane(textarea);
            
            panel.add(scrollpane);
            
            dialog.getContentPane().add(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(
                    parent,
                    "Error loading help file: "+e.getMessage(),
                    "Error loading help file",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
   
}
