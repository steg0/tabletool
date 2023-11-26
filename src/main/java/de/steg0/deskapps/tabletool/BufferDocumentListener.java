package de.steg0.deskapps.tabletool;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.steg0.deskapps.tabletool.BufferEvent.Type;

class BufferDocumentListener implements DocumentListener
{
    boolean unsaved;
    private BufferController buffer;
    
    BufferDocumentListener(BufferController buffer)
    {
        this.buffer = buffer;
        buffer.editor.getDocument().addDocumentListener(this);
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        if(!unsaved)
        {
            unsaved=true;
            buffer.fireBufferEvent(Type.CHANGED);
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        insertUpdate(e);
        if(e.getLength() == 1) return;
        ExtendTextDamageEvent.send(buffer.editor,e);
    }

    @Override public void changedUpdate(DocumentEvent e) { }
}
