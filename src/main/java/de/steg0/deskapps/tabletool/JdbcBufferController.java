package de.steg0.deskapps.tabletool;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Date;
import java.util.EventListener;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.BadLocationException;
import javax.swing.undo.UndoManager;

class JdbcBufferController
{
    static final MessageFormat FETCH_LOG_FORMAT = 
            new MessageFormat("{0} row{0,choice,0#s|1#|1<s} fetched and ResultSet {1} at {2}\n");
    static final MessageFormat FETCH_ALL_LOG_FORMAT = 
            new MessageFormat("{0,choice,0#All 0 rows|1#The only row|1<All {0} rows} fetched and ResultSet {1} at {2}\n");
    static final MessageFormat UPDATE_LOG_FORMAT = 
            new MessageFormat("{0,choice,-1#0 rows|0#0 rows|1#1 row|1<{0} rows} affected at {1}\n");

    static final Pattern QUERYPATTERN = Pattern.compile(
            "^(?:[^\\;\\-\\']*\\'[^\\']*\\'|[^\\;\\-\\']*\\-\\-[^\\n]*\\n|[^\\;\\-\\']*\\-(?!\\-))*[^\\;\\-\\']*(?:\\;|$)");
    
    interface Listener extends EventListener
    {
        void exitedNorth(JdbcBufferController source);
        void exitedSouth(JdbcBufferController source);
        void selectedRectChanged(JdbcBufferController source,Rectangle rect);
        void splitRequested(JdbcBufferController source,String text);
        void resultViewClosed(JdbcBufferController source);
    }
    
    final JFrame parent;

    JPanel panel = new JPanel(new GridBagLayout());
    
    JTextArea editor = new JTextArea();
    UndoManager undoManager = new UndoManager();
    JTable resultview;
    
    Consumer<String> log;
    
    JdbcBufferController(JFrame parent,Consumer<String> updateLog)
    {
        this.parent = parent;
        this.log = updateLog;
        
        var editorConstraints = new GridBagConstraints();
        editorConstraints.anchor = GridBagConstraints.WEST;
        panel.add(editor,editorConstraints);
        panel.setBackground(editor.getBackground());
        
        editor.addKeyListener(editorKeyListener);
        editor.getDocument().addUndoableEditListener(undoManager);
    }

    KeyListener editorKeyListener = new KeyListener()
    {
        @Override
        public void keyReleased(KeyEvent event)
        {
            switch(event.getKeyCode())
            {
            case KeyEvent.VK_ENTER:
                if(event.isControlDown()) fetch(event.isShiftDown());
            }
        }
        @Override
        public void keyPressed(KeyEvent event)
        { 
            try
            {
                switch(event.getKeyCode())
                {
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_PAGE_DOWN:
                    if(editor.getLineOfOffset(editor.getCaretPosition()) == 
                       editor.getLineCount()-1 &&
                       resultview != null)
                    {
                        for(var l : listeners.getListeners(Listener.class))
                        {
                            l.exitedSouth(JdbcBufferController.this);
                        }
                    }
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_PAGE_UP:
                    if(editor.getLineOfOffset(editor.getCaretPosition()) == 0)
                    {
                        for(var l : listeners.getListeners(Listener.class))
                        {
                            l.exitedNorth(JdbcBufferController.this);
                        }
                    }
                    break;
                case KeyEvent.VK_SLASH:
                    if(event.isControlDown()) toggleComment();
                    break;
                case KeyEvent.VK_Z:
                    if(event.isControlDown() && undoManager.canUndo())
                    {
                        undoManager.undo();
                    }
                    break;
                case KeyEvent.VK_Y:
                    if(event.isControlDown() && undoManager.canRedo())
                    {
                        undoManager.redo();
                    }
                }
            }            
            catch(BadLocationException ignored)
            {
            }
        }
        @Override public void keyTyped(KeyEvent e) { }
    };
    
    EventListenerList listeners = new EventListenerList();
    
    void addListener(Listener l)
    {
        listeners.add(Listener.class,l);
    }

    void addDocumentListener(DocumentListener l)
    {
        editor.getDocument().addDocumentListener(l);
    }
    
    void addEditorFocusListener(FocusListener f)
    {
        editor.addFocusListener(f);
    }
    
    /**
     * @param where the Y position to focus. This is understood as the
     * rightmost area of the editor in terms of X coordinates, to support
     * a sensible way of focusing from a container where the editor is
     * layouted in a left-aligned way.
     */
    void focusEditor(Integer where)
    {
        if(where != null)
        {
            var rightEdge=new Point(editor.getWidth()-1,where);
            editor.setCaretPosition(editor.viewToModel2D(rightEdge));
        }
        editor.requestFocusInWindow();
    }
    
    void appendText(String text) { editor.append(text); }
    
    void selectAll() { editor.selectAll(); }

    void prepend(JdbcBufferController c) {
        editor.setText(c.editor.getText() + "\n" + editor.getText()); 
    }
    
    void toggleComment()
    {
        var textbuf=new StringBuilder(editor.getText());
        if(textbuf.length()==0) return;
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        int caret = editor.getCaretPosition();
        if(start<0 || start==end)
        {
            /* no or zero-size selection -- treat like one char selection */
            start=(end=Math.max(1,caret))-1;
        }
        else
        {
            /* 
             * if selection started on pos 0 in line, don't include
             * this line in selection
             */
            if(textbuf.charAt(end-1)=='\n') end-=1;
        }
        /* if only part of line was selected, scan through its beginning */
        for(;start>=0;start--)
        {
            if(textbuf.charAt(start)=='\n') break;
        }
        Boolean comment=null;
        /* 
         * now determine whether to uncomment or comment, and change text
         * from bottom to top
         */
        for(int pos = end-1;pos>=start;pos--)
        {
            if(pos==-1 || textbuf.charAt(pos)=='\n')
            {
                if(Boolean.FALSE.equals(comment) ||
                   textbuf.length()>pos+2 &&
                   textbuf.substring(pos+1,pos+3).equals("--"))
                {
                    comment=false;
                    textbuf.delete(pos+1,pos+3);
                    if(pos<=caret) caret-=2;
                }
                else
                {
                    comment=true;
                    textbuf.insert(pos+1,"--");
                    if(pos<=caret) caret+=2;
                }
            }
        }
        editor.setText(textbuf.toString());
        editor.setCaretPosition(caret);
    }
    
    void store(Writer w)
    throws IOException
    {
        w.write(editor.getText());
        var rsm = getResultSetTableModel();
        if(rsm != null)
        {
            w.write('\n');
            rsm.store(w);
        }
    }
    
    String load(LineNumberReader r)
    throws IOException
    {
        var newText = new StringBuilder(editor.getText());
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
                if(newText.length()>0) newText.append("\n");
                newText.append(line);
            }
        }
        editor.getDocument().removeUndoableEditListener(undoManager);
        editor.setText(newText.toString());
        undoManager = new UndoManager();
        editor.getDocument().addUndoableEditListener(undoManager);
        return nextline;
    }
    
    int savedCaretPosition,savedSelectionStart,savedSelectionEnd;

    ConnectionWorker connection;
    int fetchsize;
    
    void fetch(boolean split)
    {
        savedCaretPosition = editor.getCaretPosition();
        String text = editor.getSelectedText() != null?
                editor.getSelectedText().trim() : selectCurrentQuery();
        savedSelectionStart = editor.getSelectionStart();
        savedSelectionEnd = editor.getSelectionEnd();
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
        
        if(split && resultview!=null)
        {
            int end = savedSelectionEnd;
            String rest = editor.getText();
            if(rest.length()>end && rest.charAt(end) == '\n') end++;
            rest = rest.substring(0,savedSelectionStart) + rest.substring(end);
            editor.setText(rest);
            for(var l : listeners.getListeners(Listener.class))
            {
                l.splitRequested(this,text);
            }
        }
        else
        {
            connection.submit(text,fetchsize,resultConsumer,updateCountConsumer,
                    log);
        }
    }

    void closeCurrentResultSet()
    {
        var model = getResultSetTableModel();
        if(model!=null && 
           connection!=null &&
           connection.lastReportedResult == model)
        {
            connection.closeResultSet(log);
        }
    }

    ResultSetTableModel getResultSetTableModel()
    {
        if(resultview != null)
        {
            return (ResultSetTableModel)resultview.getModel();
        }
        return null;
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
    
    void restoreCaretPosition()
    {
        if(editor.getSelectionStart()!=savedSelectionStart ||
           editor.getSelectionEnd()!=savedSelectionEnd) return;
        editor.setCaretPosition(savedCaretPosition);
    }

    Consumer<Integer> updateCountConsumer = (i) ->
    {
        Object[] logargs = {i,new Date().toString()};
        log.accept(UPDATE_LOG_FORMAT.format(logargs));
        restoreCaretPosition();
    };
    
    Consumer<ResultSetTableModel> resultConsumer = (rsm) ->
    {
        restoreCaretPosition();

        addResultSetTable(rsm);
        
        Object[] logargs = {
                rsm.getRowCount(),
                rsm.resultSetClosed? "closed" : "open",
                new Date().toString()
        };
        if(rsm.getRowCount() < rsm.fetchsize)
        {
            log.accept(FETCH_ALL_LOG_FORMAT.format(logargs));
        }
        else
        {
            log.accept(FETCH_LOG_FORMAT.format(logargs));
        }
    };
    
    void addResultSetTable(ResultSetTableModel rsm)
    {
        if(panel.getComponentCount()==2) panel.remove(1);

        resultview = new JTable(rsm);
        TableSizer.sizeColumns(resultview);
        
        new CellDisplayController(parent,resultview,log);
        addResultSetPopup();
        
        Dimension preferredSize = resultview.getPreferredSize();
        resultview.setPreferredScrollableViewportSize(new Dimension(
                (int)preferredSize.getWidth(),
                (int)Math.min(150,preferredSize.getHeight())));
        
        resultview.setCellSelectionEnabled(true);
        
        resultview.addKeyListener(resultsetKeyListener);
        
        var resultscrollpane = new JScrollPane(resultview,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        var resultviewConstraints = new GridBagConstraints();
        resultviewConstraints.anchor = GridBagConstraints.WEST;
        resultviewConstraints.gridy = 1;
        
        panel.add(resultscrollpane,resultviewConstraints);
        panel.revalidate();
    }
    
    void addResultSetPopup()
    {
        var popup = new JPopupMenu();
        var item = new JMenuItem("Close",KeyEvent.VK_C);
        item.addActionListener((e) ->
        {
            closeCurrentResultSet();
            resultview=null;
            panel.remove(1);
            panel.revalidate();
            for(var l : listeners.getListeners(Listener.class))
            {
                l.resultViewClosed(JdbcBufferController.this);
            }
        });
        popup.add(item);
        resultview.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) popup.show(e.getComponent(),
                        e.getX(),e.getY());
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                mousePressed(e);
            }            
        });
    }
    
    KeyListener resultsetKeyListener = new KeyListener()
    {
        void scrollToView()
        {
            Rectangle rect = editor.getBounds();
            Rectangle cellRect = resultview.getCellRect(
                    resultview.getSelectedRow(),
                    resultview.getSelectedColumn(),
                    true
            );
            Rectangle headerBounds = 
                    resultview.getTableHeader().getBounds();
            Point position = ((JViewport)resultview.getParent())
                .getViewPosition();
            for(Listener l : listeners.getListeners(Listener.class))
            {
                l.selectedRectChanged(
                        JdbcBufferController.this,
                        new Rectangle(
                                (int)cellRect.getX(),
                                (int)(rect.getHeight() + 
                                      cellRect.getY() - 
                                      position.getY() +
                                      headerBounds.getHeight()),
                                (int)cellRect.getWidth(),
                                (int)cellRect.getHeight()
                        )
                );
            }
        }
        
        @Override public void keyTyped(KeyEvent e) { }
        
        @Override
        public void keyPressed(KeyEvent e)
        {
            switch(e.getKeyCode())
            {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
                scrollToView();
            }
        }

        @Override
        public void keyReleased(KeyEvent e)
        {
            switch(e.getKeyCode())
            {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_HOME:
            case KeyEvent.VK_END:
            case KeyEvent.VK_PAGE_DOWN:
            case KeyEvent.VK_PAGE_UP:
                scrollToView();
            }
        }
    };
}
