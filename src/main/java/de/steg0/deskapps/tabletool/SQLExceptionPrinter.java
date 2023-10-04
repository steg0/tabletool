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

    static String toString(String sql,SQLException e)
    {
        StringBuilder b=new StringBuilder();
        b.append("Given:\n");
        b.append("1:");
        for(int i=0,l=1;i<sql.length();i++)
        {
            if(i%10==0) b.append("[").append(i).append(":]");
            b.append(sql.charAt(i));
            if(sql.charAt(i)=='\n') b.append(++l).append(":");
        }
        b.append("\nGot:\n");
        var msg=toString(e);
        return b+msg;
    }
}
