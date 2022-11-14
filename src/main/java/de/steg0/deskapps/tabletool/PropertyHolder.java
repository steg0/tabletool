package de.steg0.deskapps.tabletool;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Logger;

class PropertyHolder
{
    Logger logger = Logger.getLogger("tabletool.properties");

    File propertiesfile;
    private Properties properties;
    
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
            properties = new Properties();
            properties.load(propertyStream);
        }
    }
    
    Dimension getDefaultFrameSize()
    {
        String wstr = properties.getOrDefault("frame.w","700").toString();
        String hstr = properties.getOrDefault("frame.h","450").toString();
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
    
    int getScrollIncrement()
    {
        return Integer.parseInt(
                properties.getOrDefault("scroll.increment","16").toString());
    }
    
    int getResultviewHeight()
    {
        return Integer.parseInt(
                properties.getOrDefault("resultview.height","12").toString());
    }

    String getEditorFontName()
    {
        return properties.getProperty("editor.font");
    }
    
    Color getDefaultBackground()
    {
        if(!properties.containsKey("default.bg")) return null;
        return Color.decode(properties.getProperty("default.bg").toString());
    }

    class ConnectionInfo
    {
        static final String CONNECTIONS_PREFIX = "connections.";
        static final String DRIVERS_PREFIX = "drivers.";
        
        String name,url,username,password,completionTemplate,infoTemplate,
            initSql;
        final Map<String,String> snippetTemplates = new TreeMap<>();
        Color background;
        
        ConnectionInfo(String nameKey)
        {
            name=nameKey;
            logger.fine("Initializing ConnectionInfo: "+nameKey);
            String prefix=CONNECTIONS_PREFIX+nameKey;
            url=String.valueOf(properties.get(prefix+".url"));
            username=String.valueOf(properties.get(prefix+".username"));
            password=String.valueOf(properties.get(prefix+".password"));

            String driverSpec = url.replaceFirst("^jdbc\\:([a-z]+)\\:.*$","$1");
            logger.fine("Looking up templates for driver "+driverSpec);

            if(properties.containsKey(prefix+".completionTemplate"))
            {
                completionTemplate=String.valueOf(properties.get(prefix+
                        ".completionTemplate"));
            }
            else
            {
                if(properties.containsKey(
                    DRIVERS_PREFIX+driverSpec+".completionTemplate"))
                {
                    completionTemplate=String.valueOf(properties.get(
                        DRIVERS_PREFIX+driverSpec+".completionTemplate"));
                }
            }
            if(properties.containsKey(prefix+".infoTemplate"))
            {
                infoTemplate=String.valueOf(properties.get(prefix+
                        ".infoTemplate"));
            }
            else
            {
                if(properties.containsKey(
                    DRIVERS_PREFIX+driverSpec+".infoTemplate"))
                {
                    infoTemplate=String.valueOf(properties.get(
                        DRIVERS_PREFIX+driverSpec+".infoTemplate"));
                }
            }
            if(properties.containsKey(prefix+".initSql"))
            {
                initSql=String.valueOf(properties.get(prefix+".initSql"));
            }
            else
            {
                if(properties.containsKey(DRIVERS_PREFIX+driverSpec+".initSql"))
                {
                    initSql=String.valueOf(properties.get(
                        DRIVERS_PREFIX+driverSpec+".initSql"));
                }
            }
            snippetTemplates.putAll(getSnippetsForDriver(driverSpec));
            snippetTemplates.putAll(getSnippetsForConnection(nameKey));

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
                    ConnectionInfo.CONNECTIONS_PREFIX))
            .collect(groupingBy(PropertyHolder::getConnectionNameKey))
            .keySet().stream()
            .sorted()
            .map(ConnectionInfo::new)
            .toArray(ConnectionInfo[]::new);
    }

    private static String getConnectionNameKey(Object propertyKey)
    {
        String s = String.valueOf(propertyKey).substring(
                ConnectionInfo.CONNECTIONS_PREFIX.length());
        return s.substring(0,s.indexOf("."));
    }

    private Map<String,String> getSnippetsForDriver(String driverName)
    {
        return properties
            .stringPropertyNames().stream()
            .filter((k) -> String.valueOf(k).startsWith(
                    ConnectionInfo.DRIVERS_PREFIX+driverName+
                    ".snippets."))
            .collect(toMap(PropertyHolder::getSnippetNameKeyForDriver,
                    properties::getProperty));
    }
    
    private static String getSnippetNameKeyForDriver(Object propertyKey)
    {
        String s = String.valueOf(propertyKey).substring(
                ConnectionInfo.DRIVERS_PREFIX.length());
        return s.substring(s.lastIndexOf(".")+1);
    }

    private Map<String,String> getSnippetsForConnection(String connectionName)
    {
        return properties
            .stringPropertyNames().stream()
            .filter((k) -> String.valueOf(k).startsWith(
                    ConnectionInfo.CONNECTIONS_PREFIX+connectionName+
                    ".snippets."))
            .collect(toMap(PropertyHolder::getSnippetNameKeyForConnection,
                    properties::getProperty));
    }
    
    private static String getSnippetNameKeyForConnection(Object propertyKey)
    {
        String s = String.valueOf(propertyKey).substring(
                ConnectionInfo.CONNECTIONS_PREFIX.length());
        return s.substring(s.lastIndexOf(".")+1);
    }
}
