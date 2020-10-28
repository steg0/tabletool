package de.steg0.deskapps.tabletool;

import static java.util.stream.Collectors.groupingBy;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.util.Properties;

class PropertyHolder
{
    Properties properties = new Properties();
    
    void load()
    throws IOException
    {
        try(var propertyStream = getClass().getResourceAsStream(
                "/myriad.properties"))
        {
            if(propertyStream != null) properties.load(propertyStream);
        }
    }
    
    Dimension getDefaultFrameSize()
    {
        String wstr = properties.getOrDefault("frame.w","550").toString();
        String hstr = properties.getOrDefault("frame.h","300").toString();
        return new Dimension(
                Integer.parseInt(wstr),
                Integer.parseInt(hstr)
        );
    }

    Point getDefaultFrameLocation()
    {
        String xstr = properties.getOrDefault("frame.x","100").toString();
        String ystr = properties.getOrDefault("frame.y","100").toString();
        return new Point(
                Integer.parseInt(xstr),
                Integer.parseInt(ystr)
        );
    }
    
    class ConnectionInfo
    {
        static final String PROPERTY_PREFIX = "connections.";
        
        String name,url,username,password;
        
        ConnectionInfo(String nameKey)
        {
            name=nameKey;
            url=String.valueOf(properties.get(PROPERTY_PREFIX+nameKey+".url"));
            username=String.valueOf(
                    properties.get(PROPERTY_PREFIX+nameKey+".username"));
            password=String.valueOf(
                    properties.get(PROPERTY_PREFIX+nameKey+".password"));
        }
    }
    
    ConnectionInfo[] getConnections()
    {
        return properties
            .keySet().stream()
            .filter((k) -> String.valueOf(k).startsWith(
                    ConnectionInfo.PROPERTY_PREFIX))
            .collect(groupingBy(PropertyHolder::getConnectionNameKey))
            .keySet().stream()
            .sorted()
            .map(ConnectionInfo::new)
            .toArray(ConnectionInfo[]::new);
    }
    
    static String getConnectionNameKey(Object propertyKey)
    {
        String s = String.valueOf(propertyKey).substring(
                ConnectionInfo.PROPERTY_PREFIX.length());
        return s.substring(0,s.lastIndexOf("."));
    }
}
