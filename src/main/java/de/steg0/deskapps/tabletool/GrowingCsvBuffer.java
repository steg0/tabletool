package de.steg0.deskapps.tabletool;

import java.util.ArrayList;
import java.util.List;

class GrowingCsvBuffer
{
    List<Object[]> rows = new ArrayList<>();
    List<Object> row = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    
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
                    row.add(field.toString());
                    field.setLength(0);
                    break;
                case '\n':
                    row.add(field.toString());
                    field.setLength(0);
                    rows.add(row.toArray());
                    row = new ArrayList<>();
                    break;
                default:
                    state = State.FIELD;
                    field.append((char)c);
                }
                break;
            case QUOTEDFIELD:
                switch(c)
                {
                case '"':
                    state = State.QUOTEINQUOTEDFIELD;
                    break;
                default:
                    field.append((char)c);
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
                    row.add(field.toString());
                    field.setLength(0);
                    break;
                case '\n':
                    state = State.INIT;
                    row.add(field.toString());
                    field.setLength(0);
                    rows.add(row.toArray());
                    row = new ArrayList<>();
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
                    row.add(field.toString());
                    field.setLength(0);
                    break;
                case '\n':
                    state = State.INIT;
                    row.add(field.toString());
                    field.setLength(0);
                    rows.add(row.toArray());
                    row = new ArrayList<>();
                    break;
                default:
                    field.append((char)c);
                }
            }
        });
    }
}
