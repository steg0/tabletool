package de.steg0.deskapps.tabletool;

import static java.util.stream.Collectors.toMap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JTabbedPane;

class PropertyHolder
{
    Logger logger = Logger.getLogger("tabtype");

    File[] propertiesfiles;
    private Properties properties;
    
    PropertyHolder(File[] propertiesfiles)
    {
        assert propertiesfiles != null;
        this.propertiesfiles = propertiesfiles;
    }
    
    void load()
    throws IOException
    {
        properties = new Properties();
        if(propertiesfiles!=null)
        {
            for(File propertiesfile : propertiesfiles)
            {
                if(propertiesfile.exists() && propertiesfile.length() > 2) try(
                    var propertyStream = new BufferedInputStream(
                            new FileInputStream(propertiesfile)))
                {
                    if(propertiesfile.getName().endsWith(".xml"))
                    {
                        properties.loadFromXML(propertyStream);
                    }
                    else
                    {
                        properties.load(propertyStream);
                    }
                }
            }
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
        var xstr = properties.get("frame.x");
        var ystr = properties.get("frame.y");
        if(xstr==null||ystr==null) return null;
        return new Point(
                Integer.parseInt(String.valueOf(xstr)),
                Integer.parseInt(String.valueOf(ystr))
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

    Integer getEditorFontSize()
    {
        String s = properties.getProperty("editor.fontsize");
        if(s==null) return null;
        return Integer.valueOf(s);
    }

    int getEditorTabsize()
    {
        return Integer.parseInt(
                properties.getOrDefault("editor.tabsize","8").toString());
    }

    int getTabPlacement()
    {
        return Integer.parseInt(
                properties.getOrDefault("tab.placement",JTabbedPane.TOP).toString());
    }
    
    Color getDefaultBackground()
    {
        if(!properties.containsKey("default.bg")) return null;
        return Color.decode(properties.getProperty("default.bg").toString());
    }

    Color getFrameBackground()
    {
        if(!properties.containsKey("frame.bg")) return null;
        return Color.decode(properties.getProperty("frame.bg").toString());
    }

    Color getNonFocusedEditorBorderColor()
    {
        return getColorProperty("editor.nonFocusedBorder",Color.WHITE);
    }

    Color getFocusedEditorBorderColor()
    {
        return getColorProperty("editor.focusedBorder",Color.BLUE);
    }

    Color getUnsavedEditorBorderColor()
    {
        return getColorProperty("editor.unsavedBorder",Color.GRAY);
    }

    private static final String UIDEFAULTS_COLOR_PREFIX = "uiDefaults.color.";

    Object[] getColorUIDefaults()
    {
        return properties
            .stringPropertyNames().stream()
            .filter((k) -> k.startsWith(UIDEFAULTS_COLOR_PREFIX))
            .map((k) -> new Object[]{
                    k.substring(UIDEFAULTS_COLOR_PREFIX.length()),
                    Color.decode(properties.get(k).toString())
            })
            .flatMap(Arrays::stream)
            .toArray(Object[]::new);
    }

    private static final String UIDEFAULTS_GRADIENT_PREFIX =
            "uiDefaults.gradient.";

    Object[] getGradientUIDefaults()
    {
        return properties
            .stringPropertyNames().stream()
            .filter((k) -> k.startsWith(UIDEFAULTS_GRADIENT_PREFIX))
            .map((k) -> new Object[]{
                    k.substring(UIDEFAULTS_GRADIENT_PREFIX.length()),
                    Arrays.stream(properties.get(k).toString()
                            .split("\\s*,\\s*"))
                            .map(x -> x.startsWith("#")?
                                    Color.decode(x) : Double.valueOf(x))
                            .collect(Collectors.toList())
            })
            .flatMap(Arrays::stream)
            .toArray(Object[]::new);
    }

    private Color getColorProperty(String key,Color defaultColor)
    {
        if(!properties.containsKey(key)) return defaultColor;
        return Color.decode(properties.getProperty(key).toString());
    }

    String getPlaceholderRegex()
    {
        return properties.getProperty("placeholder.regex");
    }

    private static final String EXTERNAL_TOOL_DEFINITION_PREFIX =
            "externalTools.";

    ExternalToolDefinition[] getExternalToolDefinitions()
    {
        return properties
            .stringPropertyNames().stream()
            .filter((k) -> k.startsWith(EXTERNAL_TOOL_DEFINITION_PREFIX))
            .map(PropertyHolder::getExternalToolNameKey)
            .distinct()
            .map(this::getExternalToolDefinition)
            .toArray(ExternalToolDefinition[]::new);
    }

    private static String getExternalToolNameKey(Object propertyKey)
    {
        String s = String.valueOf(propertyKey).substring(
                EXTERNAL_TOOL_DEFINITION_PREFIX.length());
        return s.substring(0,s.indexOf("."));
    }

    private ExternalToolDefinition getExternalToolDefinition(
            String name)
    {
        var command = new ArrayList<String>();
        final String commandPrefix = EXTERNAL_TOOL_DEFINITION_PREFIX +
                name + ".command.";
        for(int argnum=1;properties.containsKey(commandPrefix+argnum);
            argnum++)
        {
            command.add(properties.getProperty(commandPrefix+argnum));
        }
        return new ExternalToolDefinition(name,command);
    }

    class ConnectionInfo
    {
        static final String CONNECTIONS_PREFIX = "connections.";
        static final String DRIVERS_PREFIX = "drivers.";
        
        final String name,url,username,password,completionTemplate,infoTemplate,
            initSql;
        final Map<String,String> snippetTemplates;
        final Color background;
        final boolean confirmations;
        
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

            completionTemplate = properties.containsKey(
                    prefix+".completionTemplate")? String.valueOf(
                            properties.get(prefix+".completionTemplate")) :
                    properties.containsKey(
                            DRIVERS_PREFIX+driverSpec+".completionTemplate")?
                            String.valueOf(properties.get(DRIVERS_PREFIX+
                                    driverSpec+".completionTemplate")) : null;
            
            infoTemplate = properties.containsKey(prefix+".infoTemplate")?
                    String.valueOf(properties.get(prefix+".infoTemplate")) :
                    properties.containsKey(
                            DRIVERS_PREFIX+driverSpec+".infoTemplate")?
                            String.valueOf(properties.get(DRIVERS_PREFIX+
                                    driverSpec+".infoTemplate")) : null;

            initSql = properties.containsKey(prefix+".initSql")?
                    String.valueOf(properties.get(prefix+".initSql")) :
                    properties.containsKey(DRIVERS_PREFIX+
                            driverSpec+".initSql")?
                            String.valueOf(properties.get(DRIVERS_PREFIX+
                                    driverSpec+".initSql")) : null;

            var tmap = new TreeMap<String,String>();
            tmap.putAll(getSnippetsForDriver(driverSpec));
            tmap.putAll(getSnippetsForConnection(nameKey));
            snippetTemplates = Collections.unmodifiableMap(tmap);

            background = properties.containsKey(prefix+".bg")?
                    Color.decode(String.valueOf(properties.get(
                        prefix+".bg"))) : null;
            
            confirmations = properties.containsKey(prefix+".confirmations")?
                    Boolean.valueOf(String.valueOf(properties.get(
                            prefix+".confirmations"))) : false;
        }
    
        public boolean equals(Object o)
        {
            if(!(o instanceof ConnectionInfo)) return false;
            ConnectionInfo other = (ConnectionInfo)o;
            if(!(name.equals(other.name))) return false;
            return true;
        }

        public int hashCode() { return name.hashCode(); }
    }
    
    /**@return a new array of {@link ConnectionInfo} instances parsed
     * from the property table. */
    ConnectionInfo[] getConnections()
    {
        return properties
            .keySet().stream()
            .filter((k) -> String.valueOf(k).startsWith(
                    ConnectionInfo.CONNECTIONS_PREFIX) && 
                    String.valueOf(k).endsWith(".url"))
            .map(PropertyHolder::getConnectionNameKey)
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
            .filter((k) -> 
                k.startsWith(ConnectionInfo.CONNECTIONS_PREFIX) &&
                connectionNamePatternForSnippetsMatches(k,connectionName))
            .collect(toMap(PropertyHolder::getSnippetNameKeyForConnection,
                    properties::getProperty));
    }

    private static boolean connectionNamePatternForSnippetsMatches(
            String propertyKey,String connectionName)
    {
        int rightBoundary = propertyKey.lastIndexOf(".snippets.");
        if(rightBoundary < 0) return false;
        String patternString = propertyKey.substring(
                ConnectionInfo.CONNECTIONS_PREFIX.length(),rightBoundary);
        return Pattern.compile(patternString).matcher(connectionName).matches();
    }
    
    private static String getSnippetNameKeyForConnection(String propertyKey)
    {
        String s = propertyKey.substring(
                ConnectionInfo.CONNECTIONS_PREFIX.length());
        return s.substring(s.lastIndexOf(".snippets.")+10);
    }
}
