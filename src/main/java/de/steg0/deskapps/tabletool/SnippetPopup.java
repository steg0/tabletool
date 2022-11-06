package de.steg0.deskapps.tabletool;

import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

class SnippetPopup
{
    private JdbcBufferController buffer;
    private int x,y;

    SnippetPopup(JdbcBufferController buffer,int x,int y)
    {
        this.buffer = buffer;
        this.x=x;
        this.y=y;
    }

    void show(Map<String,String> snippets)
    {
        var popup = new JPopupMenu();
        JMenuItem item;

        for(Map.Entry<String,String> snippet : snippets.entrySet())
        {
            String name = snippet.getKey();
            String completion = snippet.getValue().replaceAll("@@selection@@",
                    buffer.editor.getSelectedText());
            item = new JMenuItem(name);
            item.addActionListener((e) -> buffer.editor.replaceSelection(
                    completion));
            popup.add(item);
        }
        popup.show(buffer.editor,x,y);
   }    
}