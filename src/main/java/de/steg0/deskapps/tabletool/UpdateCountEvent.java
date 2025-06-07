package de.steg0.deskapps.tabletool;

import java.util.EventObject;

class UpdateCountEvent extends EventObject
{ 
    final long count;
    final long ms;
    /**
     * A description of JDBC IN parameters set in the dialog for the
     * execution.
     */
    final String inlog;
    /**
     * A description of JDBC OUT parameters set in the dialog for the
     * execution.
     */
    final String outlog;
    /**
     * A description of text placeholder values set in the dialog for the
     * execution.
     */
    final String placeholderlog;

    UpdateCountEvent(ConnectionWorker source,long count,long ms,
            String inlog,String outlog,String placeholderlog)
    {
        super(source);
        this.count = count;
        this.ms = ms;
        this.inlog = inlog == null? "" : inlog;
        this.outlog = outlog == null? "" : outlog;
        this.placeholderlog = placeholderlog;
    }

    @Override
    public ConnectionWorker getSource()
    {
        return (ConnectionWorker)super.getSource();
    }
}
