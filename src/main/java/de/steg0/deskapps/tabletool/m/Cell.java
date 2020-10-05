package de.steg0.deskapps.tabletool.m;


/**
 * A cell of data to be used in a tabular context.
 */
public class Cell
{

    StringBuilder val;
    String cachedDisplayVal;
    
    public Cell()
    {
    }

    public String getVal()
    {
        if(this.val==null) this.val = new StringBuilder();
        return this.val.toString();
    }
    
    public void setVal(String val)
    {
        if(this.val==null) this.val = new StringBuilder();
        this.val.setLength(0);
        this.val.append(val);
        this.invalidate();
    }
    
    public void invalidate()
    {
        this.cachedDisplayVal = null;
    }
    
    public String getDisplayVal()
    {
        if(this.cachedDisplayVal==null)
        {
            this.cachedDisplayVal = this.val.toString();
        }
        return this.cachedDisplayVal;
    }
    
    public String toString()
    {
        return this.getDisplayVal();
    }
    
}
