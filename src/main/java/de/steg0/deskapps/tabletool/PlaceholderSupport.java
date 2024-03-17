package de.steg0.deskapps.tabletool;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PlaceholderSupport
{
    private String placeholderRegex;

    PlaceholderSupport(BufferConfigSource configSource)
    {
        placeholderRegex = configSource.getPlaceholderRegex();
        if(placeholderRegex == null) placeholderRegex = "\\@\\@selection\\@\\@";
    }

    /**
     * Inserts the replacement for all occurrences of the placeholder regex
     * in <code>s</code>.
     */
    String quotedReplaceInString(String s,String replacement)
    {
        return s.replaceAll(placeholderRegex,"'"+
                replacement.replace("'","''")+"'");
    }

    /**
     * Inserts the replacement for all occurrences of the given placeholder
     * (which is not a regex) in <code>s</code>.
     */
    String quotedReplaceInString(String s,String placeholder,
            String replacement)
    {
        return s.replace(placeholder,"'"+replacement.replace("'","''")+"'");
    }

    String[] getPlaceholderOccurrences(String s)
    {
        Pattern p = Pattern.compile(placeholderRegex);
        Matcher m = p.matcher(s);
        Set<String> occurrences = new LinkedHashSet<>();
        while(m.find())
        {
            occurrences.add(m.group());
        }
        return occurrences.toArray(new String[0]);
    }
}