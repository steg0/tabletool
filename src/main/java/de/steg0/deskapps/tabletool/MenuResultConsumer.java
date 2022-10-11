package de.steg0.deskapps.tabletool;

import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

class MenuResultConsumer implements BiConsumer<ResultSetTableModel,Long>
{
    private JdbcBufferController buffer;
    private int x,y;
    private Consumer<String> log;
    int maxresults;

    MenuResultConsumer(JdbcBufferController buffer,int x,int y,
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

        for(int i=0;i<m.getRowCount()&&i<maxresults-1;i++)
        {
            String completion = String.valueOf(m.getValueAt(i,0));
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
}