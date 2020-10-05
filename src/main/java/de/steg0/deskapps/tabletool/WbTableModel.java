package de.steg0.deskapps.tabletool;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.steg0.deskapps.tabletool.m.Cell;

public class WbTableModel
implements TableModel
{
    /* 
     * The extent to be displayed. The internal storage list will often have
     * lower cardinality.
     */
    private int rows,cols;
    
    private List<ArrayList<Cell>> data;
    
    public WbTableModel(int rows,int cols)
    {
        this.rows=rows;
        this.cols=cols;
    }
    
    private Cell ensurePopulated(int rowIndex,int colIndex)
    {
        this.rows = Math.max(this.rows,rowIndex+1);
        this.cols = Math.max(this.cols,colIndex+1);
        
        if(this.data == null)
        {
            this.data = new ArrayList<ArrayList<Cell>>();
        }
        while(this.data.size()<rowIndex)
        {
            this.data.add(null);
        }
        if(this.data.size()<=rowIndex) this.data.add(new ArrayList<Cell>());
        final List<Cell> row = this.data.get(rowIndex);
        while(row.size()<colIndex)
        {
            row.add(null);
        }
        if(row.size()<=colIndex) row.add(new Cell());
        final Cell c = row.get(colIndex);
        return c;
    }
    
    private Cell getCell(int rowIndex,int colIndex)
    {
        if(this.data==null) return null;
        if(this.data.size()<=rowIndex) return null;
        final List<Cell> row = this.data.get(rowIndex);
        if(row==null) return null;
        if(row.size()<=colIndex) return null;
        final Cell c = row.get(colIndex);
        return c;
    }
    
    private void setCell(int rowIndex,int colIndex,String v)
    {
        final Cell c = this.ensurePopulated(rowIndex,colIndex);
        c.setVal(v);
    }
    
    @Override
    public int getRowCount()
    {
        return this.rows;
    }

    @Override
    public int getColumnCount()
    {
        return this.cols;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        return String.valueOf(columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex,int columnIndex)
    {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex,int columnIndex)
    {
        return this.getCell(rowIndex,columnIndex);
    }

    @Override
    public void setValueAt(Object aValue,int rowIndex,int columnIndex)
    {
        if(aValue instanceof String)
        {
            this.setCell(rowIndex,columnIndex,(String)aValue);
        }
    }

    @Override
    public void addTableModelListener(TableModelListener l)
    {
    }

    @Override
    public void removeTableModelListener(TableModelListener l)
    {
    }

}
