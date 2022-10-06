package de.steg0.deskapps.tabletool;

import java.util.function.BiConsumer;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

class MenuResultConsumer implements BiConsumer<ResultSetTableModel,Long>
{
    private JdbcBufferController buffer;
    private int x,y;

    MenuResultConsumer(JdbcBufferController buffer,int x,int y)
    {
        this.buffer = buffer;
        this.x=x;
        this.y=y;
    }

    public void accept(ResultSetTableModel m,Long count)
    {
        var popup = new JPopupMenu();
        JMenuItem item;

        for(int i=0;i<m.getRowCount()&&i<10;i++)
        {
            String completion = String.valueOf(m.getValueAt(i,0));
            item = new JMenuItem(completion.replaceFirst("^.{80}(.*)$","$1"));
            item.addActionListener((e) -> buffer.editor.insert(completion,0));
            popup.add(item);
        }

        popup.show(buffer.editor,x,y);
    }    
}