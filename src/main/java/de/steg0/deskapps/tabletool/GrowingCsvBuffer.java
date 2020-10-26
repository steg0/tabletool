package de.steg0.deskapps.tabletool;

import java.util.ArrayList;
import java.util.List;

class GrowingCsvBuffer
{
    List<Object[]> rows = new ArrayList<>();
    List<Object> row;
    StringBuilder field = new StringBuilder();
    
    enum State { INIT,FIELD,QUOTEDFIELD,QUOTEINQUOTEDFIELD };
    
    State state = State.INIT;

    /**
     * @throws IndexOutOfBoundsException if no header has been scanned
     */
    String[] getHeader()
    {
        Object[] row0 = rows.get(0);
        String[] header = new String[row0.length];
        for(int i=0;i<row0.length;i++) header[i] = (String)row0[i];
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
        StringBuilder field = new StringBuilder();
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
                    rows.add(row.toArray());
                    break;
                default:
                    state = State.FIELD;
                    field.append(c);
                }                    
                break;
            case QUOTEDFIELD:
                switch(c)
                {
                case '"':
                    state = State.QUOTEINQUOTEDFIELD;
                    break;
                default:
                    field.append(c);
                }
                break;
            case QUOTEINQUOTEDFIELD:
                switch(c)
                {
                case '"':
                    state = State.QUOTEDFIELD;
                    field.append(c);
                    break;
                case ',':
                    state = State.INIT;
                    row.add(field.toString());
                    field.setLength(0);
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
                    rows.add(row.toArray());
                    break;
                default:
                    field.append(c);
                }
            }
        });
    }
}
