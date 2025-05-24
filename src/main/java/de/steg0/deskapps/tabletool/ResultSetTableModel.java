package de.steg0.deskapps.tabletool;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

class ResultSetTableModel
implements TableModel,AutoCloseable
{
    private static final MessageFormat FETCH_INFO_FORMAT = 
            new MessageFormat("{0} row{0,choice,0#s|1#|1<s} fetched from {2} at {1}");
    private static final MessageFormat FETCH_ALL_INFO_FORMAT = 
            new MessageFormat("{0,choice,0#All 0 rows|1#The only row|1<All {0} rows} fetched from {2} at {1}");

    private Statement st;
    ResultSet rs;
    private String cols[];
    private List<Object[]> rows;
    int fetchsize;
    boolean resultSetClosed;
    String connectionDescription;
    /**
     * A description of JDBC IN parameters set in the dialog for the
     * execution.
     */
    String inlog;
    /**
     * A description of JDBC OUT parameters set in the dialog for the
     * execution.
     */
    String outlog;
    /**
     * A description of text placeholder values set in the dialog for the
     * execution.
     */
    String placeholderlog;
    /**
     * The log message associated with the last fetch operation. Empty
     * or <code>null</code> means that no message is available, either because
     * no result is available, or one was loaded back from a file that didn't
     * carry a message. Normally this value will be composed from other
     * attributes of this instance.
     */
    String resultMessage;
    Date date;
    
    /**Blockingly retrieves a ResultSet from the Statement.
     * Neither one is closed; it is expected they have to be closed
     * externally. */
    void update(String connectionDescription,Statement st,int fetchsize,
            String inlog,String outlog,String placeholderlog)
    throws SQLException
    {
        this.st = st;
        this.rs = st.getResultSet();
        this.fetchsize = fetchsize;
        this.connectionDescription = connectionDescription;
        this.inlog = inlog == null? "" : inlog;
        this.outlog = outlog == null? "" : outlog;
        this.placeholderlog = placeholderlog;
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
        date = new Date();
        Object[] logargs = {
                getRowCount(),
                date.toString(),
                connectionDescription
        };
        if(getRowCount() < fetchsize)
        {
            resultMessage = FETCH_ALL_INFO_FORMAT.format(logargs);
        }
        else
        {
            resultMessage = FETCH_INFO_FORMAT.format(logargs);
        }
        String paramlog = (inlog + outlog).trim();
        if(!paramlog.isEmpty()) paramlog = " - " + paramlog;
        if(!placeholderlog.isEmpty()) paramlog += " - " +
                placeholderlog;
        resultMessage += paramlog;
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

    /**
     * In contrast to the regular export, this exports CLOB text as well.
     * The assumption is that this will fit okay in such a more vertically
     * expanding document.
     */
    void toHtmlTransposed(Writer w) throws IOException,SQLException
    {
        var b = new StringBuilder();
        b.append("<ol>");
        for(Object[] row : rows)
        {
            b.append("<li>");
            b.append("<table>");
            for(int i=0;i<row.length;i++)
            {
                b.append("<tr>");
                b.append("<th>");
                cols[i].toString().chars().forEach((c) ->
                {
                    b.append(HtmlEscaper.nonAscii(c));
                });
                b.append("</th>");
                b.append("<td>");
                if(row[i] instanceof Clob) try(var l = new LineNumberReader(
                        ((Clob)row[i]).getCharacterStream()))
                {
                    
                    for(String line=l.readLine();line!=null;line=l.readLine())
                    {
                        line.chars().forEach((c) ->
                        {
                            b.append(HtmlEscaper.nonAscii(c));
                        });
                        b.append("<br>\n");
                    }
                }
                else if(row[i] != null)
                {
                    row[i].toString().chars().forEach((c) ->
                    {
                        b.append(HtmlEscaper.nonAscii(c));
                    });
                }
                b.append("</td>");
                b.append("</tr>");
            }
            b.append("</table>");
        }
        b.append("</ol>");
        w.write(b.toString());
    }
    
    void store(Writer w,boolean asSqlComment)
    throws IOException
    {
        if(asSqlComment) w.write("--");
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
