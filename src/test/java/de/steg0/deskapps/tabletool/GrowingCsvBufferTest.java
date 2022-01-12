package de.steg0.deskapps.tabletool;

import static org.junit.Assert.*;

import org.junit.Test;

public class GrowingCsvBufferTest
{

    @Test
    public void multilinefield()
    {
        String input = "a,\"b\nc\",d\n";
        var g = new GrowingCsvBuffer();
        g.append(input);
        String[] header = g.getHeader();
        assertEquals("a",header[0]);
        assertEquals("b\nc",header[1]);
        assertEquals("d",header[2]);
    }

    @Test
    public void quoteinfield()
    {
        String input = "a,\"b\"\"c\",d\n";
        var g = new GrowingCsvBuffer();
        g.append(input);
        String[] header = g.getHeader();
        assertEquals("a",header[0]);
        assertEquals("b\"c",header[1]);
        assertEquals("d",header[2]);
    }

    @Test
    public void quoteinunquotedfield()
    {
        String input = "a,b\"c,d\n";
        var g = new GrowingCsvBuffer();
        g.append(input);
        String[] header = g.getHeader();
        assertEquals("a",header[0]);
        assertEquals("b\"c",header[1]);
        assertEquals("d",header[2]);
    }

    @Test
    public void illegalquoteinfield()
    {
        String input = "a,\"b\"c\",d\n";
        var g = new GrowingCsvBuffer();
        try
        {
            g.append(input);
            fail("Exception expected");
        }
        catch(IllegalStateException e)
        {
        }
    }

}
