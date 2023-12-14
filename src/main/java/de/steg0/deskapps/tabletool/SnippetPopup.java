package de.steg0.deskapps.tabletool;

import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

class SnippetPopup
{
    private BufferController buffer;
    private int x,y;

    SnippetPopup(BufferController buffer,int x,int y)
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
            String selectedText = buffer.editor.getSelectedText();
            if(selectedText == null) selectedText = "";
            String completion = snippet.getValue().replace("@@selection@@",
                    selectedText);
            item = new JMenuItem(name);
            item.addActionListener((e) -> buffer.editor.replaceSelection(
                    completion));
            popup.add(item);
        }
        popup.show(buffer.editor,x,y);
   }    
}