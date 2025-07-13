package de.steg0.deskapps.tabletool;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

class NotebookLogListener extends KeyAdapter implements DocumentListener
{
    Logger logger = Logger.getLogger("tabtype");

    private NotebookController nb;

    NotebookLogListener(NotebookController notebook)
    {
        nb = notebook;
    }

    @Override public void insertUpdate(DocumentEvent e) { sizeLog(); }
    @Override public void removeUpdate(DocumentEvent e) { sizeLog(); }
    @Override public void changedUpdate(DocumentEvent e) { sizeLog(); }

    private void sizeLog()
    {
        logger.fine("Autoresizing log area to accommodate text");
        int lines = Math.min(10,nb.log.getLineCount());
        int lineheight = nb.log.getFontMetrics(nb.log.getFont()).getHeight();
        int logheight = lineheight * lines;
        logger.log(Level.FINE,"logheight={0}",logheight);
        int dividerSize = nb.logBufferPane.getDividerSize();
        logger.log(Level.FINE,"dividerSize={0}",dividerSize);
        int logBufferHeight = nb.logBufferPane.getHeight();
        logger.log(Level.FINE,"logBufferHeight={0}",logBufferHeight);
        nb.logBufferPane.setDividerLocation(logBufferHeight - logheight -
                dividerSize - (int)(lineheight * .4));
    }

    /**
     * Overrides normal F6 behavior in the split pane which would
     * just focus the top-most buffer. It's a bit more convenient
     * that way.
     */
    public void keyPressed(KeyEvent e)
    {
        if(e.getKeyCode() == KeyEvent.VK_F6 && nb.hasSavedFocusPosition)
        {
            logger.fine("F6 pressed in log, restoring buffer focus");
            nb.restoreFocus();
            e.consume();
        }
    }
}