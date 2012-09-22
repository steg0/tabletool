package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class WbTablePanel
{
    private JPanel panel;
    private JScrollPane tablePane;
    private WbTable table;

    public WbTablePanel()
    {
        this.panel = new JPanel(new BorderLayout());
        this.table = new WbTable();
        this.tablePane = new JScrollPane(this.table.getTable(),
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        this.panel.add(this.tablePane,BorderLayout.CENTER);
    }
    
    /* 
     * XXX there should be some resize listener that resizes the internal
     * table representation accordingly
     */
    
    public JPanel getPanel()
    {
        return this.panel;
    }
    
}
