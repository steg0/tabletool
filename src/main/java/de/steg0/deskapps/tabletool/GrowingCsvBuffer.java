package de.steg0.deskapps.tabletool;

import java.util.ArrayList;
import java.util.List;

class GrowingCsvBuffer
{
    final List<Object[]> rows = new ArrayList<>();
    private List<Object> row = new ArrayList<>();
    private final StringBuilder field = new StringBuilder();
    
    private enum State { INIT,FIELD,QUOTEDFIELD,QUOTEINQUOTEDFIELD };
    
    private State state = State.INIT;

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
        }
        return header;
    }
    
    List<Object[]> getRows()
    {
        return rows.subList(1,rows.size());
    }
    
    private void accept(int c)
    {
        field.append((char)c);
    }
    
    private void acceptField()
    {
        row.add(field.toString());
        field.setLength(0);
    }
    
    private void acceptLine()
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
                    accept(c);
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
    }
}
