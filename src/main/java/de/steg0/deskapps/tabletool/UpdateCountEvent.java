package de.steg0.deskapps.tabletool;

import java.util.EventObject;

class UpdateCountEvent extends EventObject
{ 
    final long count;
    final long ms;

    UpdateCountEvent(ConnectionWorker source,long count,long ms)
    {
        super(source);
        this.count = count;
        this.ms = ms;
    }

    @Override
    public ConnectionWorker getSource()
    {
        return (ConnectionWorker)super.getSource();
    }   
}
