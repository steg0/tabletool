package de.steg0.deskapps.tabletool;

import java.sql.SQLException;
import java.util.Date;

class SQLExceptionPrinter
{
    static String toString(SQLException e)
    {
        StringBuilder b=new StringBuilder();
        for(;e!=null;e=e.getNextException())
        {
            b.append("Error code: "+e.getErrorCode()+"\n");
            b.append("SQL State: "+e.getSQLState()+"\n");
            b.append(e.getMessage()+"\n");
            var cause = e.getCause();
            if(cause != null)
            {
                b.append("Caused by:\n"+cause+"\n");
            }
        }
        b.append("Error executing SQL at ");
        b.append(new Date());
        return b.toString();
    }
}
