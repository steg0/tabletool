package de.steg0.deskapps.tabletool;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

class SuffixGuess
{
    static String fromStream(InputStream is)
    throws IOException
    {
        String ct = URLConnection.guessContentTypeFromStream(is);
        String suffix=null;
        if(ct!=null)
        {
            switch(ct)
            {
            case "audio/x-wav": suffix=".wav"; break;
            case "audio/basic": suffix=".au"; break;
            default:
                suffix="."+ct.substring(ct.lastIndexOf('/')+1);
            }
        }
        else
        {
            if (!is.markSupported()) return null;

            is.mark(4);
            int c1 = is.read();
            int c2 = is.read();
            int c3 = is.read();
            int c4 = is.read();
            if(c1 == '%' && c2 == 'P' && c3 == 'D' && c4 == 'F')
            {
                suffix=".pdf";
            }
            is.reset();
        }
        return suffix;
    }
}
