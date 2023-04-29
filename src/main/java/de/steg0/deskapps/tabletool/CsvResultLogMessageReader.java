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
            String line=firstLine.substring(2);
            do
            {
                String unquoted = line.replace("\"\"","\"");
                message += unquoted;
                line = rest.readLine();
            }
            while(line.chars().filter(c -> c == '"').count() % 2 == 0);
            message = message.substring(0,message.length()-1);
            return;
        }
        message = firstLine.substring(1);
    }
}
