package de.steg0.deskapps.tabletool;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;

class UndoManagerProxy
implements UndoableEditListener
{
    UndoManager undoManager = new UndoManager();
    
    UndoManagerProxy(JTextComponent t)
    {
        t.getDocument().addUndoableEditListener(this);
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e)
    {
        undoManager.undoableEditHappened(e);
    }
    
    void tryUndo()
    {
        if(!undoManager.canUndo()) return;
        
        undoManager.undo();
    }
    
    void tryRedo()
    {
        if(!undoManager.canRedo()) return;

        undoManager.redo();
    }
}
