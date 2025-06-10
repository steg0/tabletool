package de.steg0.deskapps.tabletool;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;

class ExternalToolAction extends AbstractAction
{
    Logger logger = Logger.getLogger("tabtype");
    
    private final TabSetController tabset;
    private final ExternalToolDefinition def;

    ExternalToolAction(TabSetController tabset,ExternalToolDefinition def,
            int number)
    {
        super((number + 1) + ". " + def.name());
        this.tabset = tabset;
        this.def = def;

        int keyevent = switch(number)
        {
            case 0 -> KeyEvent.VK_1;
            case 1 -> KeyEvent.VK_2;
            case 2 -> KeyEvent.VK_3;
            case 3 -> KeyEvent.VK_4;
            case 4 -> KeyEvent.VK_5;
            case 5 -> KeyEvent.VK_6;
            case 6 -> KeyEvent.VK_7;
            case 7 -> KeyEvent.VK_8;
            case 8 -> KeyEvent.VK_9;
            case 9 -> KeyEvent.VK_0;
            default -> -1;
        };
        if(keyevent>=0) putValue(Action.MNEMONIC_KEY,keyevent);
    }

    @Override public void actionPerformed(ActionEvent event)
    {
        NotebookController notebook = tabset.getSelected();
        BufferController b = notebook.lastFocused();

        String text = b.editor.getSelectedText() != null?
                b.editor.getSelectedText() :
                b.selectCurrentQuery();
        if(text==null)
        {
            b.log.accept("No query found at " + new Date());
            return;
        }
        InputStream is=null,es=null;
        OutputStream os=null;
        try
        {
            Process p = new ProcessBuilder(def.command()).start();
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
        catch(Exception e)
        {
            b.log.accept("Error executing external command at " +
                    new Date() + ": " + e.getMessage());
            logger.log(Level.WARNING,"Error in process",e);
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
}