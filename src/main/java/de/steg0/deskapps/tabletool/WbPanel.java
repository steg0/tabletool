package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class WbPanel
{
    private JPanel panel;
    private JTabbedPane tabbedPane;
    private List<WbTablePanel> tablePanels = new ArrayList<WbTablePanel>();
    
    public WbPanel()
    {
        this.panel = new JPanel(new BorderLayout());
        this.panel.add(this.tabbedPane=new JTabbedPane(
                JTabbedPane.BOTTOM),BorderLayout.CENTER);
        this.tablePanels.add(new WbTablePanel());
        
        this.addTablePanel(new WbTablePanel());
    }
    
    protected void addTablePanel(WbTablePanel panel)
    {
        this.tablePanels.add(panel);
        this.tabbedPane.add(panel.getPanel());
    }
    
    public JPanel getPanel()
    {
        return this.panel;
    }
    
}
