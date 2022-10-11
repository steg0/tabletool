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

    MenuResultConsumer(JdbcBufferController buffer,int x,int y,
            Consumer<String> log)
    {
        this.buffer = buffer;
        this.x=x;
        this.y=y;
        this.log=log;
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

        for(int i=0;i<m.getRowCount()&&i<10;i++)
        {
            String completion = String.valueOf(m.getValueAt(i,0));
            item = new JMenuItem(completion.replaceFirst("^.{80}(.*)$","$1"));
            item.addActionListener((e) -> buffer.editor.replaceSelection(
                    completion));
            popup.add(item);
        }

        popup.show(buffer.editor,x,y);
    }    
}