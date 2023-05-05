package de.steg0.deskapps.tabletool;

import java.io.IOException;
import java.io.LineNumberReader;

class CsvResultLogMessageReader
{
    String message = "";

    void load(String firstLine,LineNumberReader rest)
    throws IOException
    {
        if(firstLine.isEmpty()) return;
        if(firstLine.startsWith(" \""))
        {
            String line=firstLine;
            var quotes = countQuotes(line);
            message += dequoteln(line);
            while(quotes % 2 == 1)
            {
                line = rest.readLine();
                quotes += countQuotes(line);
                message += dequoteln(line);
            }
            message = message.substring(0,message.length()-2);
            return;
        }
        message = firstLine.substring(1);
    }

    private long countQuotes(String line)
    {
        return line.chars().filter(c -> c == '"').count();
    }

    /**
     * Removes escaped double quotes, adds newline back (because
     * we used LineNumberReader), strips first two chars which
     * are either <code>" \""</code> or <code>"--"</code>.
     */
    private String dequoteln(String line)
    {
        return line.substring(2).replace("\"\"","\"") + "\n";
    }
}
