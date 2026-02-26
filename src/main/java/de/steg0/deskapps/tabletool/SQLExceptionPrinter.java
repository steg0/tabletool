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
            b.append("SQL State: "+e.getSQLState()+"\n");
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
                    beanInfo = Introspector.getBeanInfo(property.getClass());
                    for(var pd0 : beanInfo.getPropertyDescriptors())
                    {
                        switch(pd0.getName())
                        {
                            case "lineNumber":
                                return "lineNumber="+pd0.getReadMethod()
                                        .invoke(property)+"\n";
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            logger.log(Level.INFO,"Error during SQLException introspection",e);
        }
        return "";
    }

    static String toString(String sql,SQLException e)
    {
        StringBuilder b=new StringBuilder();
        b.append("Given:\n");
        b.append("1> ");
        for(int i=0,l=1,j=1;i<sql.length();i++,j++)
        {
            if(j%10==0) b.append(" <").append(j).append("|").append(i+1)
                    .append("> ");
            b.append(sql.charAt(i));
            if(sql.charAt(i)=='\n')
            {
                b.append(++l).append("> ");
                j=1;
            }
        }
        b.append("\nGot:\n");
        var msg=toString(e);
        return b+msg;
    }
}
