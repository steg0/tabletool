package de.steg0.deskapps.tabletool;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
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
                    var placeholderSupport = new PlaceholderSupport(b.configSource);
                    String sql = placeholderSupport.quotedReplaceInString(
                            completionTemplate,text);
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
        invokeToolAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent event)
            {
                String text = b.editor.getSelectedText();
                InputStream is=null,es=null;
                OutputStream os=null;
                try
                {
                    Process p = new ProcessBuilder("perl","-I",
                            "/cygdrive/c/Users/rsteger/config/contrib/perl-lib",
                            "-e",
                            "use SQL::Beautify;" +
                            "my $b=SQL::Beautify->new(spaces => 1,space => '\t');"+
                            "$b->add_rule('break-pop-token',')');" +
                            "$b->add_rule('break-token',['or','and']);" +
                            "while(<STDIN>){$b->add($_);};" +
                            "print $b->beautify;"
                    ).start();
                    os = p.getOutputStream();
                    os.write(text.getBytes(StandardCharsets.UTF_8));
                    os.close();
                    var out=new byte[0];
                    var err=new byte[0];
                    int outlen,errlen;
                    is = p.getInputStream();
                    es = p.getErrorStream();
                    byte[] buf = new byte[1024 * 128];
                    do
                    {
                        if((outlen=is.read(buf))>=0)
                        {
                            var newout = new byte[out.length + outlen];
                            System.arraycopy(out,0,newout,0,out.length);
                            System.arraycopy(buf,0,newout,out.length,outlen);
                            out=newout;
                        }
                        if((errlen=es.read(buf))>=0)
                        {
                            var newerr = new byte[err.length + errlen];
                            System.arraycopy(err,0,newerr,0,err.length);
                            System.arraycopy(buf,0,newerr,err.length,errlen);
                            err=newerr;
                        }
                    }
                    while(outlen>=0 || errlen>=0);
                    int exitcode = p.waitFor();
                    var outStr = new String(out,StandardCharsets.UTF_8);
                    var errStr = new String(err,StandardCharsets.UTF_8);
                    if(errStr.length()>0) b.log.accept("Process STDERR at " +
                            new Date() + ":\n" + errStr);
                    if(exitcode==0) b.editor.replaceSelection(outStr);
                }
                catch(IOException e)
                {
                    b.log.accept("Error executing external command at " +
                            new Date() + ": " + e.getMessage());
                    logger.log(Level.WARNING,"Error in process",e);
                }
                catch(InterruptedException e)
                {
                    logger.log(Level.INFO,"Wait interrupted",e);
                }
                finally
                {
                    try { if(is!=null) is.close(); }
                    catch(IOException ignored) {}
                    try { if(es!=null) es.close(); }
                    catch(IOException ignored) {}
                    try { if(os!=null) os.close(); }
                    catch(IOException ignored) {}
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
        };
}
