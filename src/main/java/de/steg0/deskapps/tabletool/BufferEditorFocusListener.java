package de.steg0.deskapps.tabletool;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

class BufferEditorFocusListener implements FocusListener
{
    private final BufferController b;

    BufferEditorFocusListener(BufferController b)
    {
        this.b = b;
    }

    @Override public void focusGained(FocusEvent e)
    {
        b.setBrandingColors();
    }

    @Override public void focusLost(FocusEvent e)
    {
        b.setBrandingColors();
    }
}