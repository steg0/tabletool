package de.steg0.deskapps.tabletool;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import javax.swing.JViewport;

import de.steg0.deskapps.tabletool.JdbcBufferEvent.Type;

class JdbcBufferResultSetKeyListener implements KeyListener
{
    private final JdbcBufferController b;

    JdbcBufferResultSetKeyListener(JdbcBufferController b)
    {
        this.b = b;
    }

    private void scrollToView()
    {
        Rectangle rect = b.editor.getBounds();
        Rectangle cellRect = b.resultview.getCellRect(
                b.resultview.getSelectedRow(),
                b.resultview.getSelectedColumn(),
                true
        );
        Rectangle headerBounds = 
                b.resultview.getTableHeader().getBounds();
        Point position = ((JViewport)b.resultview.getParent())
            .getViewPosition();
        var e = new JdbcBufferEvent(b,Type.SELECTED_RECT_CHANGED);
        e.selectedRect = new Rectangle(
                (int)cellRect.getX(),
                (int)(rect.getHeight() + 
                      cellRect.getY() - 
                      position.getY() +
                      headerBounds.getHeight()),
                (int)cellRect.getWidth(),
                (int)cellRect.getHeight()
        );
        b.fireBufferEvent(e);
    }
    
    @Override public void keyTyped(KeyEvent e) { }
    
    @Override
    public void keyPressed(KeyEvent e)
    {
        switch(e.getKeyCode())
        {
        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_RIGHT:
            scrollToView();
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        switch(e.getKeyCode())
        {
        case KeyEvent.VK_UP:
        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_RIGHT:
        case KeyEvent.VK_DOWN:
        case KeyEvent.VK_HOME:
        case KeyEvent.VK_END:
        case KeyEvent.VK_PAGE_DOWN:
        case KeyEvent.VK_PAGE_UP:
            scrollToView();
            break;
        case KeyEvent.VK_BACK_SPACE:
            if(e.isControlDown()) b.closeBuffer();
        }
    }
}
