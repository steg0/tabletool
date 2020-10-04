package de.steg0.deskapps.tabletool;

import java.io.File;

import javax.swing.JTable;

/**
 * Maintains a Swing table that represents all data. (At the moment
 * there is no partitioning of large data sets, but that may change.)
 */
public class WbTable
{
    
    private JTable table;
    
    public WbTable()
    {
        this.table = new JTable(/*new WbTableModel(100,100)*/);
        
//        this.table.
    }
    
    public JTable getTable()
    {
        return this.table;
    }
    
    public void load(File file)
    {
        //XXX
    }
    
    public void save(File file)
    {
        //XXX
    }

}
