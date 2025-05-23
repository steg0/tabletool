package de.steg0.deskapps.tabletool;

import java.text.MessageFormat;
import java.util.Date;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

class BufferUpdateCountConsumer
implements Consumer<UpdateCountEvent>
{
    private static final MessageFormat UPDATE_LOG_FORMAT = 
            new MessageFormat("{0,choice,-1#0 rows|0#0 rows|1#1 row|1<{0} rows} on {3} affected in {1} ms at {2}");

    private final JFrame parent;
    private final BufferController buffer;

    BufferUpdateCountConsumer(JFrame parent,BufferController buffer)
    {
        this.parent = parent;
        this.buffer = buffer;
    }

    @Override
    public void accept(UpdateCountEvent e)
    {
        ConnectionWorker cw = e.getSource();
        Object[] logargs = {e.count,e.ms,new Date().toString(),
                cw.info.name};
        String msg = UPDATE_LOG_FORMAT.format(logargs).trim();
        if(cw.info.confirmations)
        {
                JOptionPane.showMessageDialog(
                        parent,
                        msg +
                        "You are seeing this message because the " +
                        "connection has confirmations enabled.",
                        "Update count notification",
                        JOptionPane.WARNING_MESSAGE);

        }
        String paramlog = (e.inlog + e.outlog);
        if(!paramlog.isEmpty()) paramlog = " - " + paramlog;
        buffer.log.accept(msg + paramlog);
        buffer.restoreCaretPosition(false);
    }
}
