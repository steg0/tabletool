package de.steg0.deskapps.tabletool;

import java.awt.Rectangle;
import java.util.EventObject;

class BufferEvent extends EventObject
{
    enum Type {
        EXITED_NORTH,
        EXITED_SOUTH,
        SCROLLED_NORTH,
        SCROLLED_SOUTH,
        SCROLLED_EAST,
        SCROLLED_WEST,
        SELECTED_RECT_CHANGED,
        SPLIT_REQUESTED,
        NULL_FETCH,
        RESULT_VIEW_CLOSED,
        RESULT_VIEW_UPDATED,
        DRY_FETCH,
        CHANGED
    }
    
    Type type;
    String removedText;
    ResultSetTableModel removedRsm;
    Rectangle selectedRect;
    
    BufferEvent(BufferController source,Type type)
    {
        super(source);
        this.type = type;
    }

    @Override
    public BufferController getSource()
    {
        return (BufferController)super.getSource();
    }
}