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

    UpdateCountEvent(ConnectionWorker source,long count,long ms,
            String inlog,String outlog)
    {
        super(source);
        this.count = count;
        this.ms = ms;
        this.inlog = inlog;
        this.outlog = outlog;
    }

    @Override
    public ConnectionWorker getSource()
    {
        return (ConnectionWorker)super.getSource();
    }   
}
