package de.steg0.deskapps.tabletool;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

class ResultSetTableModel
implements TableModel,AutoCloseable
{
    Statement st;
    ResultSet rs;
    String cols[];
    List<Object[]> rows;
    int fetchsize;
    boolean resultSetClosed;
    
    /**Blockingly retrieves a ResultSet from the Statement.
     * Neither one is closed; it is expected they have to be closed
     * externally. */
    void update(Statement st,int fetchsize)
    throws SQLException
    {
        this.st = st;
        this.rs = st.getResultSet();
        this.fetchsize = fetchsize;
        fill();
    }
    
    void fill()
    throws SQLException
    {
        rows = new ArrayList<Object[]>(fetchsize);
        ResultSetMetaData m = rs.getMetaData();
        cols = new String[m.getColumnCount()];
        for(int i=1;i<=cols.length;i++)
        {
            cols[i-1] = m.getColumnLabel(i);
        }
        int rowcount=0;
        while(rowcount<fetchsize && rs.next())
        {
            Object[] row = new Object[cols.length];
            for(int i=1;i<=cols.length;i++)
            {
                row[i-1]=rs.getObject(i);
            }
            rows.add(row);
            rowcount++;
        }
        resultSetClosed = rs.isClosed();
    }
    
    void store(Writer w)
    throws IOException
    {
        w.write("--CSV Result\n--");
        for(int i=0;i<cols.length;i++)
        {
            if(i>0) w.write(',');
            String strval = sanitizeForCsv(cols[i].toString());
            w.write(strval);
        }
        w.write('\n');
        for(Object[] row : rows)
        {
            w.write("--");
            for(int i=0;i<row.length;i++)
            {
                if(i>0) w.write(',');
                if(row[i] != null)
                {
                    String strval = sanitizeForCsv(row[i].toString());
                    w.write(strval);
                }
            }
            w.write('\n');
        }
    }
    
    
    static String sanitizeForCsv(String strval)
    {
        boolean comma=strval.contains(","),nl=strval.contains("\n");
        if(comma||nl)
        {
            if(comma) strval = strval.replaceAll("\\\"","\"\"");
            if(nl) strval = strval.replaceAll("\n","\n--");
            strval = '"' + strval + '"';
        }
        return strval;
    }

    /**
     * @return the first line from <code>r</code> which follows the tabular
     * region, or <code>null</code> if the file ends there.
     * The value is expected to be processed by the caller.
     */
    String load(LineNumberReader r)
    throws IOException
    {
        try
        {
            String line,nextline=null;
            var buffer=new GrowingCsvBuffer();
            while((line=r.readLine())!=null)
            {
                if(!line.startsWith("--"))
                {
                    nextline=line;
                    break;
                }
                buffer.append(line.substring(2)+'\n');
            }
            cols = buffer.getHeader();
            rows = buffer.getRows();
            return nextline;
        }
        catch(Exception e)
        {
            throw new IOException("Error reading CSV section at line "+
                    r.getLineNumber()+": "+e.getMessage());
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

    @Override
    public void close() throws SQLException
    {
        if(rs==null) return;
        try
        {
            rs.close();
        }
        finally
        {
            st.close();
        }
    }

}
