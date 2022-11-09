package de.steg0.deskapps.tabletool;

import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

class CompletionConsumer implements BiConsumer<ResultSetTableModel,Long>
{
    private JdbcBufferController buffer;
    private int x,y;
    private Consumer<String> log;
    private int maxresults;

    CompletionConsumer(JdbcBufferController buffer,int x,int y,
            Consumer<String> log,int maxresults)
    {
        this.buffer = buffer;
        this.x=x;
        this.y=y;
        this.log=log;
        this.maxresults=maxresults;
    }

    public void accept(ResultSetTableModel m,Long count)
    {
        if(m==null || m.getRowCount() == 0)
        {
            log.accept("No completions available at "+new Date());
            if(buffer.editor.getSelectedText() != null)
            {
                buffer.editor.setSelectionStart(
                        buffer.editor.getSelectionEnd());
            }
            return;
        }
        
        var popup = new JPopupMenu();
        JMenuItem item;

        try
        {
            for(int i=0;i<m.getRowCount()&&i<maxresults-1;i++)
            {
                Object completionObj = m.getValueAt(i,0);
                String completion;
                if(completionObj instanceof Clob)
                {
                    var b = new StringBuilder();
                    var l = new LineNumberReader(
                            ((Clob)completionObj).getCharacterStream());
                    for(String line=l.readLine();line!=null;line=l.readLine())
                    {
                        if(b.length()>0) b.append('\n');
                        b.append(line);
                    }
                    completion = b.toString();
                }
                else
                {
                    completion = String.valueOf(completionObj);
                }
                String label = completion.length()>80?
                        completion.substring(0,80):completion;
                if(i==maxresults-2 && m.getRowCount()>=maxresults)
                {
                    label += " [+more...]";
                }
                item = new JMenuItem(label);
                item.addActionListener((e) -> buffer.editor.replaceSelection(
                        completion));
                popup.add(item);
            }
            popup.show(buffer.editor,x,y);
        }
        catch(SQLException e)
        {
            log.accept(SQLExceptionPrinter.toString(e));
        }
        catch(IOException e)
        {
            StringBuilder b=new StringBuilder();
            b.append("IOException occured at ");
            b.append(new Date());
            b.append(":\n");
            b.append(e.getMessage());
            log.accept(b.toString());
        }
    }    
}