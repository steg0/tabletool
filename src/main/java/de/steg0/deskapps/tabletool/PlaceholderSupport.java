package de.steg0.deskapps.tabletool;

class PlaceholderSupport
{
    private String placeholderRegex;

    PlaceholderSupport(BufferConfigSource configSource)
    {
        placeholderRegex = configSource.getPlaceholderRegex();
        if(placeholderRegex == null) placeholderRegex = "\\@\\@selection\\@\\@";
    }

    String quotedReplaceInString(String s,String replacement)
    {
        return s.replaceAll(placeholderRegex,"'"+replacement.replace("'","''")+"'");
    }
}