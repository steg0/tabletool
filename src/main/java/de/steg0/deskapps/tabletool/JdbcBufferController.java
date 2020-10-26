package de.steg0.deskapps.tabletool;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

class JdbcBufferController
implements KeyListener,FocusListener
{
    static final MessageFormat FETCH_LOG_FORMAT = 
            new MessageFormat("{0} row{0,choice,0#s|1#|1<s} fetched.\n");
    static final MessageFormat FETCH_ALL_LOG_FORMAT = 
            new MessageFormat("{0,choice,0#All 0 rows|1#The only row|1<All {0} rows} fetched.\n");

    static final Pattern QUERYPATTERN = Pattern.compile(
            "^(?:[^\\;\\-\\']*\\'[^\\']*\\'|[^\\;\\-\\']*\\-\\-[^\\n]*\\n|[^\\;\\-\\']*\\-(?!\\-))*[^\\;\\-\\']*(?:\\;|$)");
    
    JFrame parent;
    JdbcNotebookController.Actions actions;
    
    JPanel panel = new JPanel(new GridBagLayout());
    
    ConnectionWorker connection;
    
    JTextArea editor = new JTextArea();
    int savedCaretPosition;
    
    Consumer<String> log;
    
    JdbcBufferController(JFrame parent,Consumer<String> updateLog,
            JdbcNotebookController.Actions actions)
    {
        this.parent = parent;
        this.log = updateLog;
        this.actions = actions;
        
        var editorConstraints = new GridBagConstraints();
        editorConstraints.anchor = GridBagConstraints.WEST;
        panel.add(editor,editorConstraints);
        panel.setBackground(editor.getBackground());
        
        editor.addKeyListener(this);
        editor.addFocusListener(this);
    }

    @Override
    public void keyReleased(KeyEvent event)
    {
        try
        {
            switch(event.getKeyCode())
            {
            case KeyEvent.VK_ENTER:
                if(event.isControlDown()) fetch();
                break;
            case KeyEvent.VK_DOWN:
                if(editor.getLineOfOffset(editor.getCaretPosition()) == 
                   editor.getLineCount()-1 &&
                   panel.getComponentCount()>1)
                {
                    actions.nextBuffer(this);
                }
                break;
            case KeyEvent.VK_UP:
                if(editor.getLineOfOffset(editor.getCaretPosition()) == 0)
                {
                    actions.previousBuffer(this);
                }
                break;
            case KeyEvent.VK_T:
                if(event.isControlDown()) actions.newTab();
                break;
            case KeyEvent.VK_W:
                if(event.isControlDown()) actions.removeTab();
                break;
            case KeyEvent.VK_S:
                if(event.isControlDown()) actions.store();
            }
        }
        catch(BadLocationException ignored)
        {
        }
    }
    
    @Override public void keyTyped(KeyEvent e) { }
    @Override public void keyPressed(KeyEvent e) { }
    @Override public void focusGained(FocusEvent e) { }

    @Override
    public void focusLost(FocusEvent e)
    {
        actions.bufferFocusLost(this);
    }

    void focusEditor()
    {
        editor.requestFocusInWindow();
    }
    
    void appendText(String text)
    {
        editor.append(text);
    }

    void store(Writer w)
    throws IOException
    {
        w.write(editor.getText());
        if(panel.getComponentCount()==2)
        {
            w.write('\n');
            JScrollPane tablepane = (JScrollPane)panel.getComponent(1);
            JTable t = (JTable)tablepane.getViewport().getComponent(0);
            var model = (ResultSetTableModel)t.getModel();
            model.store(w);
        }
    }
    
    String load(LineNumberReader r)
    throws IOException
    {
        String line,nextline=null;
        while((line=r.readLine())!=null)
        {
            if(line.equals("--CSV Result"))
            {
                var rsm = new ResultSetTableModel();
                nextline = rsm.load(r);
                addResultSetTable(rsm);
                break;
            }
            else
            {
                editor.append(line);
                editor.append("\n");
            }
        }
        return nextline;
    }
    
    void fetch()
    {
        savedCaretPosition = editor.getCaretPosition();
        String text = editor.getSelectedText() != null?
                editor.getSelectedText().trim() : selectCurrentQuery();
        if(text == null)
        {
            log.accept("No query found at "+new Date());
            return;
        }
        else if(connection == null)
        {
            log.accept("No connection available at "+new Date());
            return;
        }
        connection.submit(text,resultConsumer,log);
    }
    
    String selectCurrentQuery()
    {
        String text = editor.getText();
        int offset = 0,position = editor.getCaretPosition();
        var m = QUERYPATTERN.matcher(text);
        while(m.find())
        {
            String match = m.group();
            if(match.trim().isEmpty()) return null;
            if(match.length() >= position) 
            {
                editor.select(offset + m.start(),offset + m.end());
                return match;
            }
            text = text.substring(match.length());
            position -= match.length();
            offset += match.length();
            m = QUERYPATTERN.matcher(text);
        }
        return null;
    }
    
    Consumer<ResultSetTableModel> resultConsumer = (rsm) ->
    {
        editor.setCaretPosition(savedCaretPosition);
        try(rsm)
        {
            addResultSetTable(rsm);
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
        catch(SQLException e)
        {
            log.accept(SQLExceptionPrinter.toString(e));
        }
    };
    
    void addResultSetTable(ResultSetTableModel rsm)
    {
        JTable resultview = new JTable(rsm);
        new CellDisplayController(parent,resultview,log);
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
    }
}
