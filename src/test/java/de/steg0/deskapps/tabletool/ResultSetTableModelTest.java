package de.steg0.deskapps.tabletool;

import static de.steg0.deskapps.tabletool.ResultSetTableModel.sanitizeForCsv;
import static org.junit.Assert.*;

import org.junit.Test;


public class ResultSetTableModelTest
{
    @Test
    public void multilinecsv()
    {
        assertEquals("\"a\n--b\"",sanitizeForCsv("a\nb",true));
    }

    @Test
    public void multilinecsv_2newlines()
    {
        assertEquals("\"a\n--b\n--\"",
                ResultSetTableModel.sanitizeForCsv("a\nb\n",true));
    }
    
    @Test
    public void multilinecsv_cr()
    {
        /* we might need to address this someday */
        assertEquals("\"a\r\n--\"",sanitizeForCsv("a\r\n",true));
    }
    
    @Test
    public void quoteinfield()
    {
        assertEquals("\"a\"\"b\"",sanitizeForCsv("a\"b",true));
    }
    
    @Test
    public void commainfield()
    {
        assertEquals("\"a,b\"",sanitizeForCsv("a,b",true));
    }
    
    @Test
    public void commaandmultiline()
    {
        assertEquals("\"a,\n--b\"",sanitizeForCsv("a,\nb",true));
    }
    
    @Test
    public void standard()
    {
        assertEquals("abc",sanitizeForCsv("abc",true));
    }

    @Test
    public void zero()
    {
        assertEquals("",sanitizeForCsv("",true));
    }
    
    @Test
    public void singlecomma()
    {
        assertEquals("\",\"",sanitizeForCsv(",",true));
    }
    
    @Test
    public void singlenewline()
    {
        assertEquals("\"\n--\"",sanitizeForCsv("\n",true));
    }
    
    @Test
    public void singlequote()
    {
        assertEquals("\"\"\"\"",sanitizeForCsv("\"",true));
    }
}
