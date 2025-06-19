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

    PropertyHolder(Properties properties)
    {
        this.properties = properties;
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
        return getColorProperty("default.bg",null);
    }

    Color getFrameBackground()
    {
        return getColorProperty("frame.bg",null);
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
        
        final String completionTemplate,infoTemplate,initSql;
        String name,url,username,password;
        final Map<String,String> snippetTemplates;
        final Color background,logBackground,logForeground,focusedBorderColor;
        final boolean confirmations;
        final boolean updatableResultSets;
        
        ConnectionInfo(String nameKey)
        {
            name=nameKey;
            logger.fine("Initializing ConnectionInfo: "+nameKey);
            String prefix=CONNECTIONS_PREFIX+nameKey;
            url=String.valueOf(properties.get(prefix+".url"));
            username=String.valueOf(properties.get(prefix+".username"));
            password=String.valueOf(properties.get(prefix+".password"));

            String driverSpec = url.replaceFirst("^jdbc\\:([a-z0-9]*)\\:.*$",
                    "$1");
            logger.fine("Looking up templates for driver "+driverSpec);

            completionTemplate = getForConnection(nameKey,
                    ".completionTemplate").getOrDefault("",getForDriver(
                            driverSpec,".completionTemplate").get(""));
            
            infoTemplate = getForConnection(nameKey,".infoTemplate")
                    .getOrDefault("",getForDriver(driverSpec,".infoTemplate")
                            .get(""));

            initSql = getForConnection(nameKey,".initSql")
                    .getOrDefault("",getForDriver(driverSpec,".initSql")
                            .get(""));

            var tmap = new TreeMap<String,String>();
            tmap.putAll(getForDriver(driverSpec,".snippets."));
            tmap.putAll(getForConnection(nameKey,".snippets."));
            snippetTemplates = Collections.unmodifiableMap(tmap);

            background = getColorProperty(prefix+".bg",null);
            logBackground = getColorProperty(prefix+".logBg",null);
            logForeground = getColorProperty(prefix+".logFg",null);
            focusedBorderColor = getColorProperty(prefix+".focusedBorderColor",
                    null);

            confirmations = properties.containsKey(prefix+".confirmations")?
                    Boolean.valueOf(String.valueOf(properties.get(
                            prefix+".confirmations"))) : false;
            updatableResultSets = properties.containsKey(prefix+
                    ".updatableResultSets")? Boolean.valueOf(String.valueOf(
                            properties.get(prefix+".updatableResultSets"))) :
                            true;
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

    private Map<String,String> getForDriver(String driverName,String type)
    {
        return properties
                .stringPropertyNames().stream()
                .filter((k) -> String.valueOf(k).startsWith(
                        ConnectionInfo.DRIVERS_PREFIX+driverName+type))
                .collect(toMap(k -> getItemNameKeyForDriver(k,driverName,
                        type),properties::getProperty));
    }
    
    private static String getItemNameKeyForDriver(Object propertyKey,
            String driverName,String type)
    {
        String s = String.valueOf(propertyKey).substring(
                ConnectionInfo.DRIVERS_PREFIX.length() + 1);
        int end = s.lastIndexOf(type);
        return end >= 0? s.substring(end + type.length()) : "";
    }

    private Map<String,String> getForConnection(String connectionName,
            String type)
    {
        return properties
                .stringPropertyNames().stream()
                .filter((k) -> 
                        k.startsWith(ConnectionInfo.CONNECTIONS_PREFIX) &&
                        connectionNamePatternMatches(k,connectionName,type))
                .collect(toMap(k -> getItemNameKeyForConnection(k,
                        connectionName,type),properties::getProperty));
    }

    private static boolean connectionNamePatternMatches(
            String propertyKey,String connectionName,String type)
    {
        int rightBoundary = propertyKey.lastIndexOf(type);
        if(rightBoundary < 0) return false;
        String patternString = propertyKey.substring(
                ConnectionInfo.CONNECTIONS_PREFIX.length(),rightBoundary);
        return Pattern.compile(patternString).matcher(connectionName).matches();
    }
    
    private static String getItemNameKeyForConnection(String propertyKey,
            String connectionName,String type)
    {
        String s = propertyKey.substring(
                ConnectionInfo.CONNECTIONS_PREFIX.length() + 1);
        int end = s.lastIndexOf(type);
        return end >= 0? s.substring(end + type.length()) : "";
    }
}