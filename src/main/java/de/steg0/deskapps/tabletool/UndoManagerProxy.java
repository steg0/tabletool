package de.steg0.deskapps.tabletool;

import java.util.Arrays;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;

class UndoManagerProxy
implements UndoableEditListener
{
    UndoManager undoManager = new UndoManager();
    
    int[] aggregateSizes = new int[150]; /* should cover UndoManager capacity */
    int pointer=0;
    boolean stopped;
    
    UndoManagerProxy(JTextComponent t)
    {
        t.getDocument().addUndoableEditListener(this);
        Arrays.fill(aggregateSizes,0);
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e)
    {
        ensureBuffer();
        undoManager.undoableEditHappened(e);
        if(stopped) aggregateSizes[pointer]++;
        else aggregateSizes[pointer++]=1;
    }
    
    void ensureBuffer()
    {
        if(pointer==aggregateSizes.length)
        {
            pointer--;
            System.arraycopy(aggregateSizes,50,aggregateSizes,0,
                    aggregateSizes.length-50);
            Arrays.fill(aggregateSizes,aggregateSizes.length-50,
                    aggregateSizes.length,0);
        }
    }
    
    void stop()
    {
        ensureBuffer();
        stopped = true;
        aggregateSizes[pointer] = 0;
    }
    
    void start()
    {
        stopped = false;
        pointer++;
    }
    
    void tryUndo()
    {
        if(stopped || !undoManager.canUndo() || pointer==0) return;
        
        int aggregate=aggregateSizes[--pointer];
        for(int i=0;i<aggregate;i++)
        {
            undoManager.undo();
        }
    }
    
    void tryRedo()
    {
        if(stopped || !undoManager.canRedo()) return;
        
        int aggregate=aggregateSizes[pointer++];
        for(int i=0;i<aggregate;i++)
        {
            undoManager.redo();
        }
    }
}
