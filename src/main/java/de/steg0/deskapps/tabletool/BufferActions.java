package de.steg0.deskapps.tabletool;

import java.awt.event.ActionEvent;
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

    Action
        executeAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                b.fetch(false);
            }
        },
        executeSplitAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                b.fetch(true);
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
