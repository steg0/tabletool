package de.steg0.deskapps.tabletool;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTextArea;

/**
 * As
 * <a href="https://stackoverflow.com/questions/60029617/java-swing-double-click-drag-to-select-whole-words"
 * >StackOverflow 60029617</a> shows, it is not required to subclass
 * <code>DefaultCaret</code> to override double-click behavior. While in
 * the event loop we can also override the default selection in a listener.
 */
class WordSelectListener
extends MouseAdapter
{
    Logger logger = Logger.getLogger("tabletool.editor");
    
    JTextArea textarea;

    WordSelectListener(JTextArea textarea)
    {
        this.textarea = textarea;
        textarea.addMouseListener(this);
        textarea.addMouseMotionListener(this);
    }

    int clickCount;
    int clickPos;
    int dragPos;

    @Override
    public void mousePressed(MouseEvent e)
    {
        clickCount = e.getClickCount(); 
        if(clickCount == 2)
        {
            clickPos = textarea.viewToModel2D(e.getPoint());
            logger.log(Level.FINE,"clickPos={0}",clickPos);
            selectWord();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if(clickCount == 2)
        {
            dragPos = textarea.viewToModel2D(e.getPoint());
            logger.log(Level.FINE,"dragPos={0}",dragPos);
            updateSelection();
        }
    }

    int getWordEndPos(String text,int initPos)
    {
        int i = initPos;
        while(i < text.length() && (
              Character.isJavaIdentifierStart(text.charAt(i)) ||
              Character.isJavaIdentifierPart(text.charAt(i))))
        {
            i++;
        }
        return i;
    }

    int getWordStartPos(String text,int initPos)
    {
        int i = initPos;
        while(i >= 0 && (
              Character.isJavaIdentifierStart(text.charAt(i)) ||
              Character.isJavaIdentifierPart(text.charAt(i))))
        {
            i--;
        }
        return i + 1;
    }

    void selectWord()
    {
        if(clickPos<0) return;
        String text = textarea.getText();
        Integer[] newSelection = {
                getWordStartPos(text,clickPos),
                getWordEndPos(text,clickPos)
        };
        logger.log(Level.FINE,"newstart={0},newend={1}",newSelection);
        textarea.select(newSelection[0],newSelection[1]);
    }
    
    void updateSelection()
    {
        if(dragPos<0) return;
        String text = textarea.getText();
        Integer[] selection = {
                max(min(clickPos,dragPos),textarea.getSelectionStart()),
                max(clickPos,textarea.getSelectionEnd())
        };
        logger.log(Level.FINE,"start={0},end={1}",selection);
        Integer[] newSelection = {
                getWordStartPos(text,selection[0]),
                getWordEndPos(text,selection[1])
        };
        logger.log(Level.FINE,"newstart={0},newend={1}",newSelection);
        textarea.select(newSelection[0],newSelection[1]);
    }
}
