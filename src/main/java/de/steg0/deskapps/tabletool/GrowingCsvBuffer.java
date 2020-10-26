package de.steg0.deskapps.tabletool;

import java.util.ArrayList;
import java.util.List;

class GrowingCsvBuffer
{
    List<Object[]> rows = new ArrayList<>();
    List<Object> row = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    int length;
    
    enum State { INIT,FIELD,QUOTEDFIELD,QUOTEINQUOTEDFIELD };
    
    State state = State.INIT;

    /**
     * @throws IndexOutOfBoundsException if no header has been scanned
     * @throws IllegalStateException if an empty header field has been scanned
     */
    String[] getHeader()
    {
        Object[] row0 = rows.get(0);
        String[] header = new String[row0.length];
        for(int i=0;i<row0.length;i++)
        {
            header[i] = (String)row0[i];
            if(header[i].length()==0) throw new IllegalStateException(
                    "header field cannot have zero length");
        }
        return header;
    }
    
    List<Object[]> getRows()
    {
        return rows.subList(1,rows.size());
    }
    
    int length()
    {
        return length;
    }

    void accept(int c)
    {
        field.append((char)c);
    }
    
    void acceptField()
    {
        row.add(field.toString());
        field.setLength(0);
    }
    
    void acceptLine()
    {
        rows.add(row.toArray());
        row = new ArrayList<>();
    }
    
    /**
     * @throws IllegalStateException if a parsing error occurs reading the
     * CSV input
     */
    void append(String s)
    {
        s.chars().forEach((c) ->
        {
            switch(state)
            {
            case INIT:
                switch(c)
                {
                case '"':
                    state = State.QUOTEDFIELD;
                    break;
                case ',':
                    acceptField();
                    break;
                case '\n':
                    acceptField();
                    acceptLine();
                    break;
                default:
                    state = State.FIELD;
                    accept(c);
                }
                break;
            case QUOTEDFIELD:
                switch(c)
                {
                case '"':
                    state = State.QUOTEINQUOTEDFIELD;
                    break;
                default:
                    accept(c);
                }
                break;
            case QUOTEINQUOTEDFIELD:
                switch(c)
                {
                case '"':
                    state = State.QUOTEDFIELD;
                    field.append((char)c);
                    break;
                case ',':
                    state = State.INIT;
                    acceptField();
                    break;
                case '\n':
                    state = State.INIT;
                    acceptField();
                    acceptLine();
                    break;
                default:
                    throw new IllegalStateException("'\"' or ',' expected");
                }
                break;
            case FIELD:
                switch(c)
                {
                case ',':
                    state = State.INIT;
                    acceptField();
                    break;
                case '\n':
                    state = State.INIT;
                    acceptField();
                    acceptLine();
                    break;
                default:
                    accept(c);
                }
            }
        });
        
        length += s.length();
    }
}
