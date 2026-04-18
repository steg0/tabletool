package de.steg0.deskapps.tabletool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.text.ParseException;
import java.util.Properties;

import org.junit.Test;

import de.steg0.deskapps.tabletool.PropertyHolder.ConnectionInfo;


public class PropertyHolderTest
{
    @Test
    public void connectionList_shouldBeSorted()
    throws ParseException
    {
        var p = new Properties();
        p.put("connections.a.url","jdbc:");
        p.put("connections.A.url","jdbc:");
        var ph = new PropertyHolder(p);
        ConnectionInfo[] connections = ph.getConnections();
        assertEquals("a",connections[1].name);
        assertEquals("A",connections[0].name);
    }

    @Test
    public void completionTemplateForConnection()
    throws ParseException
    {
        var p = new Properties();
        p.put("connections.a.url","jdbc:");
        p.put("connections.b.url","jdbc:");
        p.put("connections.A.url","jdbc:");
        p.put("connections.(a|b).completionTemplate","x");
        var ph = new PropertyHolder(p);
        ConnectionInfo[] connections = ph.getConnections();
        assertEquals("x",connections[1].completionTemplate);
        assertEquals("x",connections[2].completionTemplate);
        assertNull(connections[0].completionTemplate);
    }
    
    @Test
    public void completionTemplateForDriver()
    throws ParseException
    {
        var p = new Properties();
        p.put("connections.a.url","jdbc:a:");
        p.put("connections.b.url","jdbc::");
        p.put("drivers.a.completionTemplate","x");
        p.put("drivers..completionTemplate","y");
        var ph = new PropertyHolder(p);
        ConnectionInfo[] connections = ph.getConnections();
        assertEquals("x",connections[0].completionTemplate);
        assertEquals("y",connections[1].completionTemplate);
    }
    
    @Test
    public void completionTemplateForConnection_partOfName_nomatch()
    throws ParseException
    {
        var p = new Properties();
        p.put("connections.ca.url","jdbc:");
        p.put("connections.a.completionTemplate","x");
        var ph = new PropertyHolder(p);
        ConnectionInfo[] connections = ph.getConnections();
        assertNull(connections[0].completionTemplate);
    }
    
    @Test
    public void completionTemplateForConnection_partOfName_match()
    throws ParseException
    {
        var p = new Properties();
        p.put("connections.ca.url","jdbc:");
        p.put("connections..*a.completionTemplate","x");
        var ph = new PropertyHolder(p);
        ConnectionInfo[] connections = ph.getConnections();
        assertEquals("x",connections[0].completionTemplate);
    }
    
}
