package de.steg0.deskapps.tabletool;

import static java.util.stream.Collectors.groupingBy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

class PropertyHolder
{
    File propertiesfile;
    Properties properties = new Properties();
    
    PropertyHolder(File propertiesfile)
    {
        this.propertiesfile = propertiesfile;
    }
    
    void load()
    throws IOException
    {
        if(propertiesfile!=null) try(var propertyStream = 
                new BufferedInputStream(new FileInputStream(propertiesfile)))
        {
            properties.load(propertyStream);
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
        Color background;
        
        ConnectionInfo(String nameKey)
        {
            name=nameKey;
            String prefix=PROPERTY_PREFIX+nameKey;
            url=String.valueOf(properties.get(prefix+".url"));
            username=String.valueOf(properties.get(prefix+".username"));
            password=String.valueOf(properties.get(prefix+".password"));
            if(properties.containsKey(prefix+".bg"))
            {
                background=Color.decode(String.valueOf(properties.get(
                        prefix+".bg")));
            }
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
