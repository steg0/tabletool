package de.steg0.deskapps.tabletool;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GrowingCsvBufferTest
{

    @Test
    void multilinefield()
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
    void quoteinfield()
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
    void quoteinunquotedfield()
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
    void illegalquoteinfield()
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
