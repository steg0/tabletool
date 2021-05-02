package de.steg0.deskapps.tabletool;

import static de.steg0.deskapps.tabletool.ResultSetTableModel.sanitizeForCsv;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ResultSetTableModelTest
{
    @Test
    public void multilinecsv()
    {
        assertEquals("\"a\n--b\"",sanitizeForCsv("a\nb"));
    }

    @Test
    public void multilinecsv_2newlines()
    {
        assertEquals("\"a\n--b\n--\"",
                ResultSetTableModel.sanitizeForCsv("a\nb\n"));
    }
    
    @Test
    public void multilinecsv_cr()
    {
        /* we might need to address this someday */
        assertEquals("\"a\r\n--\"",sanitizeForCsv("a\r\n"));
    }
    
    @Test
    public void quoteinfield()
    {
        assertEquals("\"a\"\"b\"",sanitizeForCsv("a\"b"));
    }
    
    @Test
    public void commainfield()
    {
        assertEquals("\"a,b\"",sanitizeForCsv("a,b"));
    }
    
    @Test
    public void commaandmultiline()
    {
        assertEquals("\"a,\n--b\"",sanitizeForCsv("a,\nb"));
    }
    
    @Test
    public void standard()
    {
        assertEquals("abc",sanitizeForCsv("abc"));
    }

    @Test
    public void zero()
    {
        assertEquals("",sanitizeForCsv(""));
    }
    
    @Test
    public void singlecomma()
    {
        assertEquals("\",\"",sanitizeForCsv(","));
    }
    
    @Test
    public void singlenewline()
    {
        assertEquals("\"\n--\"",sanitizeForCsv("\n"));
    }
    
    @Test
    public void singlequote()
    {
        assertEquals("\"\"\"\"",sanitizeForCsv("\""));
    }
}
