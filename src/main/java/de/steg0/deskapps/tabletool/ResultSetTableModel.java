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
    private Statement st;
    ResultSet rs;
    private String cols[];
    private List<Object[]> rows;
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
    
    private void fill()
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
    
    String toHtml()
    {
        var b = new StringBuilder();
        b.append("<table><tr>");
        for(int i=0;i<cols.length;i++)
        {
            b.append("<th>");
            cols[i].toString().chars().forEach((c) ->
            {
                b.append(HtmlEscaper.nonAscii(c));
            });
            b.append("</th>");
        }
        b.append("</tr>");
        for(Object[] row : rows)
        {
            b.append("<tr>");
            for(int i=0;i<row.length;i++)
            {
                b.append("<td>");
                if(row[i] != null)
                {
                    row[i].toString().chars().forEach((c) ->
                    {
                        b.append(HtmlEscaper.nonAscii(c));
                    });
                }
                b.append("</td>");
            }
            b.append("</tr>");
        }
        b.append("</table>");
        return b.toString();
    }
    
    void store(Writer w,boolean asSqlComment)
    throws IOException
    {
        if(asSqlComment) w.write("--CSV Result\n--");
        for(int i=0;i<cols.length;i++)
        {
            if(i>0) w.write(',');
            String strval = sanitizeForCsv(cols[i].toString(),asSqlComment);
            w.write(strval);
        }
        w.write('\n');
        for(Object[] row : rows)
        {
            if(asSqlComment) w.write("--");
            for(int i=0;i<row.length;i++)
            {
                if(i>0) w.write(',');
                if(row[i] != null)
                {
                    String strval = sanitizeForCsv(row[i].toString(),
                            asSqlComment);
                    w.write(strval);
                }
            }
            w.write('\n');
        }
        w.write('\n');
    }
    
    static String sanitizeForCsv(String strval,boolean asSqlComment)
    {
        StringBuilder out=null;
        for(int i=0;i<strval.length();i++)
        {
            switch(strval.charAt(i))
            {
            case ',':
                if(out==null) out=new StringBuilder(strval.substring(0,i));
                out.append(',');
                break;
            case '"':
                if(out==null) out=new StringBuilder(strval.substring(0,i));
                out.append("\"\"");
                break;
            case '\n':
                if(out==null) out=new StringBuilder(strval.substring(0,i));
                out.append("\n");
                if(asSqlComment) out.append("--");
                break;
            default:
                if(out!=null) out.append(strval.charAt(i));
            }
        }
        return out==null? strval : "\"" + out + "\"";
    }

    /**Always expects the SQL comment format. */
    void load(LineNumberReader r)
    throws IOException
    {
        try
        {
            String line=null;
            var buffer=new GrowingCsvBuffer();
            while((line=r.readLine())!=null)
            {
                if(!line.startsWith("--"))
                {
                    if(line.length()>0) throw new IOException(
                            "Empty line expected");
                    break;
                }
                buffer.append(line.substring(2)+'\n');
            }
            cols = buffer.getHeader();
            rows = buffer.getRows();
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
        try
        {
            return rows.get(rowIndex)[columnIndex];
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            throw new ArrayIndexOutOfBoundsException(
                    "Table data incomplete in row "+rowIndex+
                    ": "+e.getMessage());
        }
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
    
    boolean isClosed() throws SQLException
    {
        return rs==null || rs.isClosed();
    }

    /**
     * Right now this is only called from {@link ConnectionWorker} on
     * its executor, so it's serial with other operations on the connection.
     */
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
            rs=null;
            st.close();
        }
    }
}
