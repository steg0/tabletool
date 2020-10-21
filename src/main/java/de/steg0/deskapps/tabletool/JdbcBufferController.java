package de.steg0.deskapps.tabletool;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Date;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

class JdbcBufferController
implements KeyListener
{
    static final MessageFormat UPDATE_LOG_FORMAT = 
            new MessageFormat("{0} row{0,choice,0#s|1#|1<s} affected.\n");
    static final MessageFormat FETCH_LOG_FORMAT = 
            new MessageFormat("{0} row{0,choice,0#s|1#|1<s} fetched.\n");
    static final MessageFormat FETCH_ALL_LOG_FORMAT = 
            new MessageFormat("{0,choice,0#All 0 rows|1#The only row|1<All {0} rows} fetched.\n");

    static final Pattern QUERYPATTERN = Pattern.compile(
            "^(?:[^\\;\\-\\']*\\'[^\\']*\\'|[^\\;\\-\\']*\\-\\-[^\\n]*\\n|[^\\;\\-\\']*\\-(?!\\-))*[^\\;\\-\\']*(?:\\;|$)");
    
    JPanel panel = new JPanel(new GridBagLayout());
    Connection connection;
    JTextArea editor = new JTextArea();
    Consumer<String> log;
    JdbcNotebookController.Actions actions;
    JdbcNotebookController notebook;
    
    JdbcBufferController(Consumer<String> updateLog,
            JdbcNotebookController.Actions actions)
    {
        this.log = updateLog;
        this.actions = actions;
        
        var editorConstraints = new GridBagConstraints();
        editorConstraints.anchor = GridBagConstraints.WEST;
        panel.add(editor,editorConstraints);
        
        editor.addKeyListener(this);
    }

    @Override
    public void keyReleased(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
        case KeyEvent.VK_ENTER:
            if(event.isControlDown()) fetch();
            break;
        case KeyEvent.VK_DOWN:
            if(event.isControlDown() && panel.getComponentCount()>1)
            {
                actions.nextBuffer(this);
            }
            break;
        case KeyEvent.VK_UP:
            if(event.isControlDown()) actions.previousBuffer(this);
        }
    }
    
    @Override public void keyTyped(KeyEvent e) { }
    @Override public void keyPressed(KeyEvent e) { }
    
    void focusEditor()
    {
        editor.requestFocusInWindow();
    }

    /**blocking */
    void fetch()
    {
        String text = editor.getSelectedText() != null?
                editor.getSelectedText().trim() : getCurrentQuery();
        if(text == null)
        {
            log.accept("No query found at "+new Date()+".\n");
            return;
        }
        else
        {
            if(text.endsWith(";")) text = text.substring(0,text.length()-1);
        }
        try(Statement st = connection.createStatement())
        {
            if(st.execute(text))
            {
                try(ResultSet rs = st.getResultSet())
                {
                    ResultSetTableModel rsm = new ResultSetTableModel();
                    rsm.update(rs);
                    JTable resultview = new JTable(rsm);
                    Dimension preferredSize = resultview.getPreferredSize();
                    resultview.setPreferredScrollableViewportSize(new Dimension(
                            (int)preferredSize.getWidth(),
                            (int)Math.min(150,preferredSize.getHeight())));
                    var resultscrollpane = new JScrollPane(resultview,
                            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                    if(panel.getComponentCount()==2) panel.remove(1);
                    var resultviewConstraints = new GridBagConstraints();
                    resultviewConstraints.anchor = GridBagConstraints.WEST;
                    resultviewConstraints.gridy = 1;
                    panel.add(resultscrollpane,resultviewConstraints);
                    panel.revalidate();
                    if(rsm.getRowCount() < ResultSetTableModel.FETCHSIZE)
                    {
                        log.accept(FETCH_ALL_LOG_FORMAT.format(new Object[]{
                                rsm.getRowCount()}));
                    }
                    else
                    {
                        log.accept(FETCH_LOG_FORMAT.format(new Object[]{
                                rsm.getRowCount()}));
                    }
                }
            }
            else
            {
                log.accept(UPDATE_LOG_FORMAT.format(new Object[]{
                        st.getUpdateCount()}));
            }
        }
        catch(SQLException e)
        {
            StringBuilder b=new StringBuilder();
            b.append("Error executing SQL at ");
            b.append(new Date());
            b.append("\n");
            for(;e!=null;e=e.getNextException())
            {
                b.append("Error code: "+e.getErrorCode()+"\n");
                b.append("SQL State: "+e.getSQLState()+"\n");
                b.append(e.getMessage()+"\n");
            }
            log.accept(b.toString());
        }
        catch(NullPointerException e)
        {
            StringBuilder b=new StringBuilder();
            b.append("No connection available at ");
            b.append(new Date());
            b.append("\n");
            log.accept(b.toString());
        }
    }
    
    String getCurrentQuery()
    {
        String text = editor.getText();
        int currentPosition = editor.getCaretPosition();
        var m = QUERYPATTERN.matcher(text);
        while(m.find())
        {
            String match = m.group();
            if(match.trim().isEmpty()) return null;
            if(match.length() >= currentPosition) return match;
            text = text.substring(match.length());
            currentPosition -= match.length();
            m = QUERYPATTERN.matcher(text);
        }
        return null;
    }
}
