package de.steg0.deskapps.tabletool;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

class NotebookBufferPaneMouseListener extends MouseAdapter
{
    Logger logger = Logger.getLogger("tabtype");
    
    private NotebookController nb;

    NotebookBufferPaneMouseListener(NotebookController notebook)
    {
        nb = notebook;
    }

    int clickVpY;
    Component clickBuffer;
    
    @Override
    public void mouseClicked(MouseEvent e)
    {
        int vpY = e.getY() + (int)nb.bufferPane.getViewport()
                .getViewPosition().getY();
        logger.log(Level.FINE,"mouseClicked,vpY={0}",vpY);
        var component = nb.bufferPanel.getComponentAt(new Point(0,vpY));
        for(var buffer : nb.buffers)
        {
            if(buffer.panel == component)
            {
                int bufferY = (int)buffer.panel.getLocation().getY();
                int y = vpY - bufferY;
                if(e.getClickCount() == 3)
                {
                    buffer.dragLineSelection(y,y);
                }
                else if(e.isShiftDown() && buffer.editor.hasFocus())
                {
                    buffer.dragLineSelection(-1,y);
                }
                else if(!e.isShiftDown())
                {
                    buffer.focusEditor(null,y);
                    buffer.startLineSelection(y);
                }
                return;
            }
        }
        BufferController lastBuffer = nb.buffers.get(nb.buffers.size() - 1);
        if(lastBuffer.resultview != null)
        {
            nb.exitedSouth(lastBuffer);
        }
        else
        {
            int bufferY = (int)lastBuffer.panel.getLocation().getY();
            lastBuffer.focusEditor(null,vpY - bufferY);
        }
    }
    
    @Override
    public void mousePressed(MouseEvent e)
    {
        if(e.isShiftDown()) return;
        int bufY = (int)nb.bufferPane.getViewport().getViewPosition().getY();
        clickVpY = bufY + e.getY();
        logger.log(Level.FINE,"mousePressed,clickVpY={0}",clickVpY);
        clickBuffer = nb.bufferPanel.getComponentAt(new Point(0,clickVpY));
        if(!nb.buffers.stream().anyMatch((b) -> b.panel==clickBuffer))
        {
            logger.fine("Click beyond last buffer");
            clickBuffer=nb.buffers.get(nb.buffers.size() - 1).panel;
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent e)
    {
        int vpY = e.getY() + (int)nb.bufferPane.getViewport()
                .getViewPosition().getY();
        logger.log(Level.FINE,"mouseDragged,vpY={0}",vpY);
        var component = nb.bufferPanel.getComponentAt(new Point(0,vpY));
        if(component != clickBuffer) return;
        for(var buffer : nb.buffers)
        {
            if(buffer.panel == component)
            {
                int bufferY = (int)buffer.panel.getLocation().getY();
                buffer.dragLineSelection(clickVpY - bufferY,vpY - bufferY);
                return;
            }
        }
    }
}