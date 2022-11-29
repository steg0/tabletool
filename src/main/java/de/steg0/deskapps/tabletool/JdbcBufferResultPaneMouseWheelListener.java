package de.steg0.deskapps.tabletool;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JViewport;

import de.steg0.deskapps.tabletool.JdbcBufferEvent.Type;

class JdbcBufferResultPaneMouseWheelListener
implements MouseWheelListener
{
    private final JdbcBufferController b;

    JdbcBufferResultPaneMouseWheelListener(JdbcBufferController b)
    {
        this.b = b;
    }

    MouseWheelListener originalListener;

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        int wr = e.getWheelRotation();
        JViewport vp = (JViewport)b.resultview.getParent();
        if(wr > 0)
        {
            if(e.isShiftDown() && vp.getViewPosition().getX() + 
                    vp.getWidth() >= b.resultview.getWidth())
            {
                b.fireBufferEvent(Type.SCROLLED_EAST);
            }
            else if(vp.getViewPosition().getY() + vp.getHeight() >=
                    b.resultview.getHeight())
            {
                b.fireBufferEvent(Type.SCROLLED_SOUTH);
            }
            else
            {
                originalListener.mouseWheelMoved(e);
            }
        }
        else if(wr < 0)
        {
            if(e.isShiftDown() && vp.getViewPosition().getX() == 0)
            {
                b.fireBufferEvent(Type.SCROLLED_WEST);
            }
            else if(vp.getViewPosition().getY() == 0)
            {
                b.fireBufferEvent(Type.SCROLLED_NORTH);
            }
            else
            {
                originalListener.mouseWheelMoved(e);
            }
        }
    }
}