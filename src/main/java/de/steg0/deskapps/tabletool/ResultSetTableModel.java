package de.steg0.deskapps.tabletool;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class ResultSetTableModel
implements TableModel
{

    ResultSet rs;
    String cols[];
    List<Object[]> rows;
    
    void update(ResultSet rs)
    throws SQLException
    {
        this.rs = rs;
        fill();
    }
    
    void fill()
    throws SQLException
    {
        rows = new ArrayList<Object[]>(10);
        ResultSetMetaData m = rs.getMetaData();
        cols = new String[m.getColumnCount()];
        for(int i=1;i<=cols.length;i++)
        {
            cols[i-1] = m.getColumnLabel(i);
        }
        int rowcount=0;
        while(rs.next() && rowcount<10)
        {
            Object[] row = new Object[cols.length];
            for(int i=1;i<=cols.length;i++)
            {
                row[i-1]=rs.getObject(i);
            }
            rows.add(row);
            rowcount++;
        }
    }
    
    @Override
    public int getRowCount()
    {
        return rows.size();
    }

    @Override
    public int getColumnCount()
    {
        return cols.length;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        return cols[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex,int columnIndex)
    {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex,int columnIndex)
    {
        return rows.get(rowIndex)[columnIndex];
    }

    @Override
    public void setValueAt(Object aValue,int rowIndex,int columnIndex)
    {
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
