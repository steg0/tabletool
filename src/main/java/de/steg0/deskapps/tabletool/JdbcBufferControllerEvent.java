package de.steg0.deskapps.tabletool;

import java.awt.Rectangle;
import java.util.EventObject;

@SuppressWarnings("serial")
class JdbcBufferControllerEvent extends EventObject
{
    enum Type {
        EXITED_NORTH,
        EXITED_SOUTH,
        SCROLLED_NORTH,
        SCROLLED_SOUTH,
        SELECTED_RECT_CHANGED,
        SPLIT_REQUESTED,
        RESULT_VIEW_CLOSED,
        RESULT_VIEW_UPDATED,
        DRY_FETCH
    }
    
    Type type;
    String text;
    int selectionStart,selectionEnd;
    Rectangle selectedRect;
    
    JdbcBufferControllerEvent(JdbcBufferController source,Type type)
    {
        super(source);
        this.type = type;
    }

    @Override
    public JdbcBufferController getSource()
    {
        return (JdbcBufferController)super.getSource();
    }
}