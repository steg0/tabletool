package de.steg0.deskapps.tabletool;

import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.undo.CompoundEdit;

/**
 * See
 * <a href="https://stackoverflow.com/questions/24433089/jtextarea-settext-undomanager"
 * >StackOverflow 24433089</a>
 */
@SuppressWarnings("serial")
class GroupableUndoDocument extends PlainDocument
{
    private CompoundEdit compoundEdit;

    @Override
    protected void fireUndoableEditUpdate(UndoableEditEvent e)
    {
        if(compoundEdit == null)
        {
            super.fireUndoableEditUpdate(e);
        }
        else
        {
            compoundEdit.addEdit(e.getEdit());
        }
    }

    @Override
    public void replace(int offset,int length,String text,AttributeSet attrs)
            throws BadLocationException
    {
        if(length == 0)
        {
            super.replace(offset,length,text,attrs);
        }
        else
        {
            startCompoundEdit();
            super.replace(offset,length,text,attrs);
            endCompoundEdit();
        }
    }
    
    void startCompoundEdit()
    {
        compoundEdit = new CompoundEdit();
        super.fireUndoableEditUpdate(
                new UndoableEditEvent(this,compoundEdit));
    }
    
    void endCompoundEdit()
    {
        compoundEdit.end();
        compoundEdit = null;
    }
}
