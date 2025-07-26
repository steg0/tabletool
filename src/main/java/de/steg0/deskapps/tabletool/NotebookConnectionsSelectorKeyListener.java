package de.steg0.deskapps.tabletool;

import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

class NotebookConnectionsSelectorKeyListener extends KeyAdapter
{
    Logger logger = Logger.getLogger("tabtype");

    private NotebookController notebook;
    private JFrame parent;

    NotebookConnectionsSelectorKeyListener(NotebookController notebook,
            JFrame parent)
    {
        this.notebook = notebook;
        this.parent = parent;
    }

    public void keyTyped(KeyEvent e)
    {
        char c = e.getKeyChar();
        if(e.getModifiersEx() != 0 && e.getModifiersEx() != SHIFT_DOWN_MASK) return;
        if(c != KeyEvent.CHAR_UNDEFINED && Character.isLetterOrDigit(c))
        {
            e.consume();
            var connectionDialog = new OpenConnectionDialogController(
                    notebook,parent);
            logger.log(Level.FINE,"Picking connection for letter {0}",c);
            connectionDialog.pick(String.valueOf(c));
        }
    }
}
