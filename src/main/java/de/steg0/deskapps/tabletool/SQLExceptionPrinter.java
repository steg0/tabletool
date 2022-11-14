package de.steg0.deskapps.tabletool;

import java.sql.SQLException;
import java.util.Date;

class SQLExceptionPrinter
{
    static String toString(SQLException e)
    {
        StringBuilder b=new StringBuilder();
        b.append("Error executing SQL at ");
        b.append(new Date());
        b.append("\n");
        var cause = e.getCause();
        if(cause != null && cause != e)
        {
            b.append(cause+"\n");
        }
        for(;e!=null;e=e.getNextException())
        {
            b.append("Error code: "+e.getErrorCode()+"\n");
            b.append("SQL State: "+e.getSQLState()+"\n");
            b.append(e.getMessage()+"\n");
        }
        return b.toString();
    }
}
