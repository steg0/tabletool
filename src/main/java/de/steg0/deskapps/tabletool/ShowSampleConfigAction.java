package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.Font;
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
        try(var r=new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/sample.properties.xml"),
                StandardCharsets.UTF_8)))
        {
            var dialog = new JDialog(parent,"Sample Configuration - Tabtype");
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            
            var panel = new JPanel(new BorderLayout());
            
            panel.setBorder(new EmptyBorder(10,10,10,10));
            
            var textarea = new JTextArea(30,77);
            textarea.setEditable(false);
            textarea.setLineWrap(true);
            textarea.setWrapStyleWord(true);

            textarea.setFont(new Font(
                    Font.MONOSPACED,
                    Font.PLAIN,
                    textarea.getFont().getSize()));
            
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
                    "Error loading sample configuration file: "+e.getMessage(),
                    "Error loading sample configuration file",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
   
}