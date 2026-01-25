package de.steg0.deskapps.tabletool;

import static java.awt.event.ActionEvent.ALT_MASK;
import static java.awt.event.ActionEvent.CTRL_MASK;
import static java.awt.event.ActionEvent.SHIFT_MASK;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;

import de.steg0.deskapps.tabletool.BufferEvent.Type;

class BufferActions
{
    Logger logger = Logger.getLogger("tabtype");

    private final BufferController b;
    private final JFrame parent;

    BufferActions(JFrame parent,BufferController b)
    {
        this.parent = parent;
        this.b = b;
    }

    void attach()
    {
        var im = b.editor.getInputMap();
        im.put(getKeyStroke(KeyEvent.VK_F5,0),"Execute");
        im.put(getKeyStroke(KeyEvent.VK_R,CTRL_MASK),"Execute");
        im.put(getKeyStroke(KeyEvent.VK_ENTER,ALT_MASK),"JDBC Parameters");
        im.put(getKeyStroke(KeyEvent.VK_ENTER,CTRL_MASK),"Execute/Split");
        im.put(getKeyStroke(KeyEvent.VK_MINUS,CTRL_MASK|SHIFT_MASK),"Split");
        im.put(getKeyStroke(KeyEvent.VK_F1,0),"Show Info");
        im.put(getKeyStroke(KeyEvent.VK_F2,0),"Show Snippets");
        im.put(getKeyStroke(KeyEvent.VK_F8,0),"Show Completions");
        im.put(getKeyStroke(KeyEvent.VK_SLASH,CTRL_MASK),"Toggle Comment");
        im.put(getKeyStroke(KeyEvent.VK_Z,CTRL_MASK),"Undo");
        im.put(getKeyStroke(KeyEvent.VK_Y,CTRL_MASK),"Redo");
        im.put(getKeyStroke(KeyEvent.VK_G,CTRL_MASK),"Go To Line");
        im.put(getKeyStroke(KeyEvent.VK_D,CTRL_MASK),"Delete Line");
        var am = b.editor.getActionMap();
        am.put("Execute",executeAction);
        am.put("JDBC Parameters",showJdbcParametersAction);
        am.put("Execute/Split",executeSplitAction);
        am.put("Split",splitAction);
        am.put("Show Info",showInfoAction);
        am.put("Show Snippets",showSnippetsPopupAction);
        am.put("Show Completions",showCompletionPopupAction);
        am.put("Toggle Comment",new EditorPrefixToggler(b.editor,"--"));
        am.put("Undo",undoAction);
        am.put("Redo",redoAction);
        am.put("Go To Line",goToLineAction);
        am.put("Delete Line",deleteLineAction);
    }

    Action
        executeAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                b.submit(true,false);
            }
        },
        executeSplitAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                b.submit(true,true);
            }
        },
        splitAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                b.submit(false,true);
            }
        },
        showJdbcParametersAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                b.parametersController.setVisible(true);
            }
        },
        showInfoAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent event)
            {
                if(b.connection == null)
                {
                    b.log.accept("No connection available at "+new Date());
                    b.fireBufferEvent(Type.DRY_FETCH);
                    return;
                }
                String infoTemplate = b.configSource.getInfoTemplate();
                if(infoTemplate == null)
                {
                    b.log.accept("No infoTemplate available at "+new Date());
                    return;
                }
                String text = b.editor.getSelectedText();
                if(text == null)
                {
                    b.selectListener.clickPos = b.editor.getCaretPosition();
                    b.selectListener.selectWord();
                    text = b.editor.getSelectedText();
                }
                if(text == null) return;
                logger.fine("Fetching info for text: "+text);
                int maxresults = 10000;
                var placeholderSupport = new PlaceholderSupport(b.configSource);
                String sql = placeholderSupport.quotedReplaceInString(
                    infoTemplate,text);
                logger.fine("Info using SQL: "+sql);
                b.connection.submit(sql,maxresults,null,null,
                        b.infoResultConsumer,b.updateCountConsumer,b.log,
                        true,false);
            }
        },
        showSnippetsPopupAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent event)
            {
                if(b.connection == null)
                {
                    b.log.accept("No connection available at "+new Date());
                    b.fireBufferEvent(Type.DRY_FETCH);
                    return;
                }
                Map<String,String> snippetTemplates =
                    b.configSource.getSnippetTemplates();
                if(snippetTemplates.size()==0)
                {
                    b.log.accept("No snippetTemplates available at "+
                            new Date());
                    return;
                }
                String text = b.editor.getSelectedText();
                if(text == null)
                {
                    b.selectListener.clickPos = b.editor.getCaretPosition();
                    b.selectListener.selectWord();
                    text = b.editor.getSelectedText();
                }
                if(text == null) text = "";
                logger.fine("Completing text: "+text);
                try
                {
                    var xy = b.editor.modelToView2D(
                            b.editor.getCaretPosition());
                    new SnippetPopup(b,
                            (int)xy.getCenterX(),(int)xy.getCenterY())
                            .show(snippetTemplates);
                }
                catch(BadLocationException e)
                {
                    assert false : e.getMessage();
                }
            }
        },
        showCompletionPopupAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent event)
            {
                if(b.connection == null)
                {
                    b.log.accept("No connection available at "+new Date());
                    b.fireBufferEvent(Type.DRY_FETCH);
                    return;
                }
                String completionTemplate = b.configSource
                    .getCompletionTemplate();
                if(completionTemplate == null)
                {
                    b.log.accept("No completionTemplate available at "+
                            new Date());
                    return;
                }
                try
                {
                    String text = b.editor.getSelectedText();
                    if(text == null)
                    {
                        b.selectListener.clickPos = b.editor.getCaretPosition();
                        b.selectListener.selectWord();
                        text = b.editor.getSelectedText();
                    }
                    if(text == null) return;
                    logger.fine("Completing text: "+text);
                    var xy = b.editor.modelToView2D(
                            b.editor.getCaretPosition());
                    int maxresults = 16;
                    var resultConsumer = new CompletionConsumer(b,
                                (int)xy.getCenterX(),(int)xy.getCenterY(),
                                b.log,maxresults);
                    var placeholderSupport = new PlaceholderSupport(
                            b.configSource);
                    String sql = placeholderSupport.quotedReplaceInString(
                            completionTemplate,text);
                    logger.fine("Completion using SQL: "+sql);
                    b.connection.submit(sql,maxresults,null,null,resultConsumer,
                            b.updateCountConsumer,b.log,false,false);
                }
                catch(BadLocationException e)
                {
                    assert false : e.getMessage();
                }
            }
        },
        undoAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                if(b.undoManager.canUndo()) b.undoManager.undo();
            }
        },
        redoAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                if(b.undoManager.canRedo()) b.undoManager.redo();
            }
        },
        goToLineAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent event)
            {
                String text = JOptionPane.showInputDialog(parent,"Go to line:");
                if(text!=null) try
                {
                    int line = Integer.parseInt(text);
                    int position = b.editor.getLineStartOffset(line - 1);
                    b.editor.setCaretPosition(position);        
                }
                catch(Exception e)
                {
                    JOptionPane.showMessageDialog(
                        parent,
                        "Error navigating: "+e.getMessage(),
                        "Error navigating",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        },
        deleteLineAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent event)
            {
                int caret = b.editor.getCaretPosition();
                try
                {
                    int line = b.editor.getLineOfOffset(caret);
                    int start = b.editor.getLineStartOffset(line);
                    int end = b.editor.getLineEndOffset(line);
                    b.editor.replaceRange("",start,end);
                }
                catch(BadLocationException e)
                {
                    assert false;
                }
            }
        };
}
