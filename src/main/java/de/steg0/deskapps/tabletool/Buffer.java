package de.steg0.deskapps.tabletool;

import java.awt.FlowLayout;
import java.sql.ResultSet;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTable;

public class Buffer
{
    JPanel panel = new JPanel(new FlowLayout());
    
    {
        JEditorPane ep = new JEditorPane();
        panel.add(ep);
    }
    
    void addResults(ResultSet rs)
    {
        JTable t = new JTable();
        panel.add(t);
    }
}
