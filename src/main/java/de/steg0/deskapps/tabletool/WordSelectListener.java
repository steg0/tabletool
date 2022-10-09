package de.steg0.deskapps.tabletool;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTextArea;

/**
 * As <a href=
 * "https://stackoverflow.com/questions/60029617/java-swing-double-click-drag-to-select-whole-words"
 * >StackOverflow 60029617</a> shows, it is not required to subclass
 * <code>DefaultCaret</code> to override double-click behavior. While in the
 * event loop we can override the default selection in a listener. This class
 * also does it for triple-click.
 * 
 * <p>
 * 
 * The number of clicks used to start the selection, as well as character
 * positions, are maintained in the instance to be used by
 * {@link JdbcNotebookController} too. The reason for this is that a mouse
 * operation might also have been started in the right panel area next to the
 * editor. Such actions should interoperate as well as possible.
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

    /**
     * <table border>
     * <tr><td>1</td><td>extend selection by characters at a time</td></tr>
     * <tr><td>2</td><td>extend selection by words at a time (defined by
     * Java identifier characters)</td></tr>
     * <tr><td>3</td><td>extend selection by lines at a time</td></tr>
     * </table>
     */
    int clickCount;
    /**
     * The position, in character index, where the selection was started.
     */
    int clickPos;
    /**
     * The position, in character index, where the mouse drag operation extends
     * to.
     */
    int dragPos;

    @Override
    public void mousePressed(MouseEvent e)
    {
        switch(e.getClickCount())
        {
        case 1:
            if(e.isShiftDown() && clickCount > 1)
            {
                mouseDragged(e);
            }
            else
            {
                clickCount = e.getClickCount();
                clickPos = textarea.viewToModel2D(e.getPoint());
            }
            break;
        case 2:
            clickCount = e.getClickCount();
            clickPos = textarea.viewToModel2D(e.getPoint());
            logger.log(Level.FINE,"selectWord,clickPos={0}",clickPos);
            selectWord();
            break;
        case 3:
            clickCount = e.getClickCount();
            clickPos = textarea.viewToModel2D(e.getPoint());
            logger.log(Level.FINE,"selectLine,clickPos={0}",clickPos);
            selectLine();
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e)
    {
        if(textarea.getSelectedText() == null) clickCount = e.getClickCount();
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        dragPos = textarea.viewToModel2D(e.getPoint());
        updateSelection();
    }
    
    void updateSelection()
    {
        switch(clickCount)
        {
        case 2:
            logger.log(Level.FINE,"updateWordSelection,dragPos={0}",dragPos);
            updateWordSelection();
            break;
        case 3:
            logger.log(Level.FINE,"updateLineSelection,dragPos={0}",dragPos);
            updateLineSelection();
        }
    }

    int getWordStartPos(String text,int initPos)
    {
        if(text.length()==0) return 0;
        int i = min(initPos,text.length()-1);
        while(i >= 0 && (
              Character.isJavaIdentifierStart(text.charAt(i)) ||
              Character.isJavaIdentifierPart(text.charAt(i))))
        {
            i--;
        }
        return i + 1;
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
    
    void updateWordSelection()
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

    int getLineStartPos(String text,int initPos)
    {
        int i = initPos;
        if(text.charAt(i) == '\n')
        {
            i--;
        }
        while(i >= 0 && text.charAt(i) != '\n')
        {
            i--;
        }
        return i + 1;
    }

    int getLineEndPos(String text,int initPos)
    {
        int i = initPos;
        while(i < text.length() && text.charAt(i) != '\n')
        {
            i++;
        }
        return i;
    }

    void selectLine()
    {
        if(clickPos<0) return;
        String text = textarea.getText();
        Integer[] newSelection = {
                getLineStartPos(text,clickPos),
                getLineEndPos(text,clickPos)
        };
        logger.log(Level.FINE,"newstart={0},newend={1}",newSelection);
        textarea.select(newSelection[0],newSelection[1]);
    }
    
    void updateLineSelection()
    {
        if(dragPos<0) return;
        String text = textarea.getText();
        Integer[] selection = {
                max(min(clickPos,dragPos),textarea.getSelectionStart()),
                max(clickPos,textarea.getSelectionEnd())
        };
        logger.log(Level.FINE,"start={0},end={1}",selection);
        Integer[] newSelection = {
                getLineStartPos(text,selection[0]),
                getLineEndPos(text,selection[1])
        };
        logger.log(Level.FINE,"newstart={0},newend={1}",newSelection);
        textarea.select(newSelection[0],newSelection[1]);
    }
}
