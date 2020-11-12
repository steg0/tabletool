package de.steg0.deskapps.tabletool;

class HtmlEscaper
{
    static String nonAscii(int c)
    {
        if(c!=34 && c!=60 && c!=62 && c < 128) return(String.valueOf((char)c));
        else return "&#"+c+";";
    }
}
