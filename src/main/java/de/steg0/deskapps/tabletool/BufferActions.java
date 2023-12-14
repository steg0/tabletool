package de.steg0.deskapps.tabletool;

import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.text.BadLocationException;

import de.steg0.deskapps.tabletool.BufferEvent.Type;

class BufferActions
{
    Logger logger = Logger.getLogger("tabletool.editor");

    private final BufferController b;

    BufferActions(BufferController b)
    {
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
                String sql = infoTemplate.replaceAll("@@selection@@",
                        text.replaceAll("'","''"));
                logger.fine("Info using SQL: "+sql);
                b.connection.submit(sql,maxresults,
                        b.infoResultConsumer,b.updateCountConsumer,b.log);
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
                    String sql = completionTemplate.replaceAll(
                            "@@selection@@",text.replaceAll("'","''"));
                    logger.fine("Completion using SQL: "+sql);
                    b.connection.submit(sql,maxresults,resultConsumer,
                            b.updateCountConsumer,b.log);
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
                b.togglePrefix("--",null);
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
        };
}
