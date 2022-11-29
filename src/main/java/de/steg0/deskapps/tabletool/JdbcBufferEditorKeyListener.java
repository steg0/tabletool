package de.steg0.deskapps.tabletool;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.text.BadLocationException;

import de.steg0.deskapps.tabletool.JdbcBufferEvent.Type;

class JdbcBufferEditorKeyListener implements KeyListener
{
    private final JdbcBufferController b;

    JdbcBufferEditorKeyListener(JdbcBufferController b)
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
                if(b.editor.getSelectionEnd()!=b.editor.getSelectionStart())
                {
                    b.togglePrefix("\t",!event.isShiftDown());
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