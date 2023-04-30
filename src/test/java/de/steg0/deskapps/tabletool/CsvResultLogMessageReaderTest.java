package de.steg0.deskapps.tabletool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;

import org.junit.Test;

public class CsvResultLogMessageReaderTest
{
    @Test
    public void multilineField() throws IOException
    {
        var r = new CsvResultLogMessageReader();
        var lr = new LineNumberReader(new StringReader("--b\n--\"\ndropped"));
        r.load(" \"a",lr);
        assertEquals("a\nb\n",r.message);
    }

    @Test
    public void multilineFieldWithExtraContent() throws IOException
    {
        /* not really supported but shouldn't do any harm either. */
        var r = new CsvResultLogMessageReader();
        var lr = new LineNumberReader(new StringReader("--b\n--\"kept\ndropped"));
        r.load(" \"a",lr);
        assertEquals("a\nb\n\"kep",r.message);
    }

    @Test
    public void multilineFieldWithInnerQuotes() throws IOException
    {
        var r = new CsvResultLogMessageReader();
        var lr = new LineNumberReader(new StringReader("--\"\"b\n--\"\"\""));
        r.load(" \"a\"\"",lr);
        assertEquals("a\"\n\"b\n\"",r.message);
    }

    @Test
    public void runawayQuote() throws IOException
    {
        var r = new CsvResultLogMessageReader();
        var lr = new LineNumberReader(new StringReader("--b\n"));
        try
        {
            r.load(" \"a",lr);
            fail("Exception expected");
        }
        catch(Exception e)
        {
            System.out.printf("OK, got %s\n",e);
        }
    }

    @Test
    public void singleLine() throws IOException
    {
        var r = new CsvResultLogMessageReader();
        r.load(" a",null);
        assertEquals("a",r.message);
    }

    @Test
    public void singleLineWithInnerQuotes() throws IOException
    {
        var r = new CsvResultLogMessageReader();
        r.load(" \"a\"\"b\"",null);
        assertEquals("a\"b",r.message);
    }
}
