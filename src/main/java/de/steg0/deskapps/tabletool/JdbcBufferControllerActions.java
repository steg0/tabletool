package de.steg0.deskapps.tabletool;

import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.text.BadLocationException;

import de.steg0.deskapps.tabletool.JdbcBufferControllerEvent.Type;

class JdbcBufferControllerActions
{
    Logger logger = Logger.getLogger("tabletool.editor");

    JdbcBufferController buffer;

    Action
        executeAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                buffer.fetch(false);
            }
        },
        executeSplitAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                buffer.fetch(true);
            }
        },
        showInfoAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent event)
            {
                if(buffer.connection == null)
                {
                    buffer.log.accept("No connection available at "+new Date());
                    buffer.fireBufferEvent(Type.DRY_FETCH);
                    return;
                }
                String infoTemplate = buffer.configSource.getInfoTemplate();
                if(infoTemplate == null)
                {
                    buffer.log.accept("No infoTemplate available at "+
                            new Date());
                    return;
                }
                String text = buffer.editor.getSelectedText();
                if(text == null)
                {
                    buffer.selectListener.clickPos =
                        buffer.editor.getCaretPosition();
                    buffer.selectListener.selectWord();
                    text = buffer.editor.getSelectedText();
                }
                if(text == null) return;
                logger.fine("Fetching info for text: "+text);
                int maxresults = 10000;
                String sql = infoTemplate.replaceAll("@@selection@@",text);
                logger.fine("Info using SQL: "+sql);
                buffer.connection.submit(sql,maxresults,
                        buffer.infoResultConsumer,buffer.updateCountConsumer,
                        buffer.log);
            }
        },
        showSnippetsPopupAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent event)
            {
                if(buffer.connection == null)
                {
                    buffer.log.accept("No connection available at "+new Date());
                    buffer.fireBufferEvent(Type.DRY_FETCH);
                    return;
                }
                Map<String,String> snippetTemplates =
                    buffer.configSource.getSnippetTemplates();
                if(snippetTemplates.size()==0)
                {
                    buffer.log.accept("No snippetTemplates available at "+
                            new Date());
                    return;
                }
                String text = buffer.editor.getSelectedText();
                if(text == null)
                {
                    buffer.selectListener.clickPos =
                        buffer.editor.getCaretPosition();
                    buffer.selectListener.selectWord();
                    text = buffer.editor.getSelectedText();
                }
                if(text == null) text = "";
                logger.fine("Completing text: "+text);
                try
                {
                    var xy = buffer.editor.modelToView2D(
                            buffer.editor.getCaretPosition());
                    new SnippetPopup(buffer,
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
                if(buffer.connection == null)
                {
                    buffer.log.accept("No connection available at "+new Date());
                    buffer.fireBufferEvent(Type.DRY_FETCH);
                    return;
                }
                String completionTemplate = buffer.configSource
                    .getCompletionTemplate();
                if(completionTemplate == null)
                {
                    buffer.log.accept("No completionTemplate available at "+
                            new Date());
                    return;
                }
                try
                {
                    String text = buffer.editor.getSelectedText();
                    if(text == null)
                    {
                        buffer.selectListener.clickPos =
                            buffer.editor.getCaretPosition();
                        buffer.selectListener.selectWord();
                        text = buffer.editor.getSelectedText();
                    }
                    if(text == null) return;
                    logger.fine("Completing text: "+text);
                    var xy = buffer.editor.modelToView2D(
                            buffer.editor.getCaretPosition());
                    int maxresults = 16;
                    var resultConsumer =
                        new CompletionConsumer(buffer,
                                (int)xy.getCenterX(),(int)xy.getCenterY(),
                                buffer.log,maxresults);
                    String sql = completionTemplate.replaceAll(
                            "@@selection@@",text);
                    logger.fine("Completion using SQL: "+sql);
                    buffer.connection.submit(sql,maxresults,resultConsumer,
                            buffer.updateCountConsumer,buffer.log);
                }
                catch(BadLocationException e)
                {
                    assert false : e.getMessage();
                }
            }
        },
        toggleCommentAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                buffer.togglePrefix("--",null);
            }
        },
        undoAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                if(buffer.undoManager.canUndo()) buffer.undoManager.undo();
            }
        },
        redoAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                if(buffer.undoManager.canRedo()) buffer.undoManager.redo();
            }
        };
}
