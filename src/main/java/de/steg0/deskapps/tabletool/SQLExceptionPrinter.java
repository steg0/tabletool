package de.steg0.deskapps.tabletool;

import java.beans.Introspector;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

class SQLExceptionPrinter
{
    static Logger logger = Logger.getLogger("tabtype");

    static String toString(SQLException e)
    {
        StringBuilder b=new StringBuilder();
        for(;e!=null;e=e.getNextException())
        {
            b.append("Error code: "+e.getErrorCode()+"\n");
            b.append("SQL state: "+e.getSQLState()+"\n");
            b.append(describeVendorSpecificProperties(e));
            b.append(e.getMessage()+"\n");
            var cause = e.getCause();
            if(cause != null)
            {
                b.append("Caused by:\n"+cause+"\n");
            }
        }
        b.append("Driver reported error at ");
        b.append(new Date());
        return b.toString();
    }

    private static String describeVendorSpecificProperties(SQLException se)
    {
        try
        {
            var beanInfo = Introspector.getBeanInfo(se.getClass());
            for(var pd : beanInfo.getPropertyDescriptors())
            {
                switch(pd.getName())
                {
                case "SQLServerError":
                    var property = pd.getReadMethod().invoke(se);
                    return describeMSSQLExceptionProperties(property);
                }
            }
        }
        catch(Exception e)
        {
            logger.log(Level.INFO,"Error during SQLException introspection",e);
        }
        return "";
    }

    private static String describeMSSQLExceptionProperties(Object property)
    throws Exception
    {
        var beanInfo = Introspector.getBeanInfo(property.getClass());
        for(var pd0 : beanInfo.getPropertyDescriptors())
        {
            switch(pd0.getName())
            {
            case "lineNumber":
                return "Line number: "+pd0.getReadMethod()
                        .invoke(property)+"\n";
            }
        }
        return "";
    }

    static String toString(String sql,SQLException e)
    {
        int maxlinelen=0;
        int line=1;
        for(int i=0,j=0;i<sql.length();i++)
        {
            if(sql.charAt(i)=='\n')
            {
                line++;
                maxlinelen = Math.max(maxlinelen,j);
                j=0;
            }
            else
            {
                j++;
            }
        }

        String linenumfmt = "%0"+String.valueOf(line).length()+"d> ";
        String charposfmt = " <%0"+String.valueOf(maxlinelen).length()+
                "d|"+"%0"+String.valueOf(sql.length()).length()+"d> ";

        StringBuilder b=new StringBuilder();
        b.append("Given:\n");
        b.append(linenumfmt.formatted(1));

        for(int i=0,l=1,j=0;i<sql.length();i++)
        {
            if(j>0 && j%10==9) b.append(charposfmt.formatted(j+1,i+1));
            b.append(sql.charAt(i) == '\t'? ' ' : sql.charAt(i));
            if(sql.charAt(i)=='\n')
            {
                b.append(linenumfmt.formatted(++l));
                j=0;
            }
            else
            {
                j++;
            }
        }
        b.append("\nGot:\n");
        var msg=toString(e);
        return b+msg;
    }
}
