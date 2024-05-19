package de.steg0.deskapps.tabletool;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.text.BadLocationException;

import de.steg0.deskapps.tabletool.BufferEvent.Type;

class BufferEditorKeyListener implements KeyListener
{
    private final BufferController b;

    BufferEditorKeyListener(BufferController b)
    {
        this.b = b;
    }

    @Override
    public void keyPressed(KeyEvent event)
    { 
        try
        {
            int caret = b.editor.getCaretPosition();
            switch(event.getKeyCode())
            {
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_PAGE_DOWN:
                if(event.isShiftDown()) break;
                if(event.isAltDown()) break;
                if(b.editor.getLineOfOffset(caret) ==
                   b.editor.getLineCount()-1 &&
                   b.resultview != null)
                {
                    b.fireBufferEvent(Type.EXITED_SOUTH);
                }
                else if(event.getKeyCode()==KeyEvent.VK_PAGE_DOWN)
                {
                    b.traverseScreenful(caret,1);
                    event.consume();
                }
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_PAGE_UP:
                if(event.isShiftDown()) break;
                if(event.isAltDown()) break;
                if(b.editor.getLineOfOffset(caret) == 0)
                {
                    b.fireBufferEvent(Type.EXITED_NORTH);
                }
                else if(event.getKeyCode()==KeyEvent.VK_PAGE_UP)
                {
                    b.traverseScreenful(caret-1,-1);
                    event.consume();
                }
                break;
            case KeyEvent.VK_TAB:
                if(event.isShiftDown() ||
                   b.editor.getSelectionEnd()!=b.editor.getSelectionStart())
                {
                    new EditorPrefixToggler(b.editor,"\t").toggle(
                            !event.isShiftDown());
                    event.consume();
                }
            }
        }            
        catch(BadLocationException e)
        {
            assert false : e.getMessage();
        }
    }
    
    @Override public void keyReleased(KeyEvent e) { }
    @Override public void keyTyped(KeyEvent e) { }
}