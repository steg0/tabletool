package de.steg0.deskapps.tabletool;

class JdbcNotebookSearchState
{
    String text;
    int tab;
    int buf;
    int loc;
    void reset(int tab)
    {
        this.tab=tab;
        buf=0;
        loc=-1;
    }
}