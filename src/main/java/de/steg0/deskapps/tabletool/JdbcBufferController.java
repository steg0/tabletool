package de.steg0.deskapps.tabletool;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Date;
import java.util.EventListener;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import de.steg0.deskapps.tabletool.JdbcBufferControllerEvent.Type;

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
        void bufferActionPerformed(JdbcBufferControllerEvent e);
    }
    
    Logger logger = Logger.getLogger("tabletool.editor");

    final JFrame parent;

    JPanel panel = new JPanel(new GridBagLayout());
    
    JTextArea editor = new JTextArea(new GroupableUndoDocument());
    
    {
        new WordSelectListener(editor);
    }
    
    /**The system-default editor background */
    final Color defaultBackground = editor.getBackground();
    UndoManagerProxy undoManagerProxy = new UndoManagerProxy(editor); 
    JTable resultview;
    int resultviewHeight;
    
    Consumer<String> log;
    
    JdbcBufferController(JFrame parent,Consumer<String> updateLog,
            int resultviewHeight)
    {
        this.parent = parent;
        this.resultviewHeight = resultviewHeight;
        this.log = updateLog;
        
        var editorConstraints = new GridBagConstraints();
        editorConstraints.anchor = GridBagConstraints.WEST;
        panel.add(editor,editorConstraints);
        panel.setBackground(defaultBackground);
        
        editor.addKeyListener(editorKeyListener);
    }
    
    void setBackground(Color background)
    {
        editor.setBackground(background);
        panel.setBackground(background);
    }
    
    Color getBackground()
    {
        return editor.getBackground();
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
                int caret = editor.getCaretPosition();
                switch(event.getKeyCode())
                {
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_PAGE_DOWN:
                    if(event.isShiftDown()) break;
                    if(editor.getLineOfOffset(caret) == 
                       editor.getLineCount()-1 &&
                       resultview != null)
                    {
                        fireBufferEvent(Type.EXITED_SOUTH);
                    }
                    else if(event.getKeyCode()==KeyEvent.VK_PAGE_DOWN)
                    {
                        traverseScreenful(caret,1);
                        event.consume();
                    }
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_PAGE_UP:
                    if(event.isShiftDown()) break;
                    if(editor.getLineOfOffset(caret) == 0)
                    {
                        fireBufferEvent(Type.EXITED_NORTH);
                    }
                    else if(event.getKeyCode()==KeyEvent.VK_PAGE_UP)
                    {
                        traverseScreenful(caret-1,-1);
                        event.consume();
                    }
                    break;
                case KeyEvent.VK_SLASH:
                    if(event.isControlDown()) togglePrefix("--",null);
                    break;
                case KeyEvent.VK_TAB:
                    if(editor.getSelectionEnd()!=editor.getSelectionStart())
                    {
                        togglePrefix("\t",!event.isShiftDown());
                        event.consume();
                    }
                    break;
                case KeyEvent.VK_Z:
                    if(event.isControlDown()) undoManagerProxy.tryUndo();
                    break;
                case KeyEvent.VK_Y:
                    if(event.isControlDown()) undoManagerProxy.tryRedo();
                }
            }            
            catch(BadLocationException e)
            {
                assert false : e.getMessage();
            }
        }
        @Override public void keyTyped(KeyEvent e) { }
    };
    
    static JViewport findViewportParent(Component c)
    {
        if(c==null) return null;
        if(c instanceof JViewport) return (JViewport)c;
        return findViewportParent(c.getParent());
    }
    
    int getLineHeight()
    {
        return editor.getHeight() / editor.getLineCount();
    }
    
    int getCaretPositionInLine()
    {
        String t = editor.getText();
        int caret = editor.getCaretPosition();
        int index = t.lastIndexOf('\n',caret-1);
        if(index<0) return caret;
        return caret-1-index;
    }
    
    void setCaretPositionInLine(int position)
    {
        String t = editor.getText();
        int caretInLine = getCaretPositionInLine();
        int caretBeginning = editor.getCaretPosition() - caretInLine;
        int lastIndex = t.indexOf('\n',caretBeginning);
        if(lastIndex<0) lastIndex = t.length();
        int newPosition = Math.min(lastIndex,caretBeginning + position);
        editor.setCaretPosition(newPosition);
        editor.setSelectionStart(newPosition);
        editor.setSelectionEnd(newPosition);
    }
    
    void traverseScreenful(int start,int vec)
    {
        int vpheight = findViewportParent(editor).getHeight();
        int linesOnScreen = vpheight / getLineHeight();
        String t = editor.getText();
        int i=start,nl=0,offset=getCaretPositionInLine();
        for(;i>=0&&i<t.length()&&nl<linesOnScreen;i += vec)
        {
            if(t.charAt(i)=='\n') nl++;
        }
        editor.setCaretPosition(Math.max(0,i));
        setCaretPositionInLine(offset);
    }

    EventListenerList listeners = new EventListenerList();
    
    void addListener(Listener l)
    {
        listeners.add(Listener.class,l);
    }
    
    void fireBufferEvent(JdbcBufferControllerEvent e)
    {
        for(var l : listeners.getListeners(Listener.class))
        {
            l.bufferActionPerformed(e);
        }
    }

    void fireBufferEvent(Type type)
    {
        fireBufferEvent(new JdbcBufferControllerEvent(this,type));
    }

    void addDocumentListener(DocumentListener l)
    {
        editor.getDocument().addDocumentListener(l);
    }
    
    /**
     * @param characterX
     *            the X position to set the caret to, which is a character
     *            position relative to the start of the line that has the caret.
     * @param y
     *            the Y position (in point units) to set the caret to. If
     *            negative, measures from the bottom of the editor area. If
     *            provided without <code>characterX</code>, this will result in
     *            the rightmost possible caret position of the resulting line.
     */
    void focusEditor(Integer characterX,Integer pointY)
    {
        if(pointY != null)
        {
            var p=new Point(
                    editor.getWidth() - 1,
                    pointY>=0? pointY : editor.getHeight() - pointY
            );
            int position=Math.max(0,editor.viewToModel2D(p));
            editor.setSelectionEnd(position);
            editor.setSelectionStart(position);
            editor.setCaretPosition(position);
        }
        if(characterX != null)
        {
            setCaretPositionInLine(characterX);
        }
        editor.requestFocusInWindow();
    }
    
    void dragLineSelection(int y1,int y2)
    {
        logger.log(Level.FINE,"dragLineSelection,y1={0}",y1);
        logger.log(Level.FINE,"dragLineSelection,y2={0}",y2);
        
        focusEditor(null,null);
        
        int start,end;
        
        if(y1<y2)
        {
            start = editor.viewToModel2D(new Point(0,y1));
            end = editor.viewToModel2D(new Point(0,y2 + getLineHeight()));
            
            editor.select(start,end);
        }
        else
        {
            start = editor.viewToModel2D(new Point(0,y2));
            end = editor.viewToModel2D(new Point(0,y1 + getLineHeight()));

            editor.setSelectionStart(start);
            editor.setSelectionEnd(end);
            editor.setCaretPosition(end);
            editor.moveCaretPosition(start);
        }
    }
    
    void prepend(JdbcBufferController c) {
        /* Use Document API so that the editor does not request a viewport
         * change. */
        try
        {
            editor.getDocument().insertString(0,c.editor.getText() + "\n",null);
        }
        catch(BadLocationException e)
        {
            assert false : e.getMessage();
        }
    }
    
    void togglePrefix(String prefix,Boolean add)
    {
        int plen = prefix.length();
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        try
        {
            if(start<0 || start==end)
            {
                /* no or zero-size selection -- treat like one char selection */
                start=(end=Math.max(1,editor.getCaretPosition()))-1;
            }
            else
            {
                /* 
                 * if selection started on pos 0 in line, don't include
                 * this line in selection
                 */
                if(editor.getText(end-1,1).equals("\n")) end-=1;
            }
            /* if only part of line was selected, scan through its beginning */
            for(;start>=0;start--)
            {
                if(editor.getText(start,1).equals("\n")) break;
            }
            /* 
             * now determine whether to uncomment or comment, and change text
             * from bottom to top
             */
            ((GroupableUndoDocument)editor.getDocument()).startCompoundEdit();
            for(int pos = end-1;pos>=start;pos--)
            {
                if(pos==-1 || editor.getText(pos,1).equals("\n"))
                {
                    boolean hasPrefix=editor.getText().length()>pos+plen &&
                            editor.getText(pos+1,plen).equals(prefix);
                    if(add==null) add=!hasPrefix;
                    if(Boolean.FALSE.equals(add))
                    {
                        add=false;
                        if(hasPrefix) editor.getDocument().remove(pos+1,plen);
                    }
                    else
                    {
                        add=true;
                        editor.getDocument().insertString(pos+1,prefix,null);
                    }
                }
            }
            ((GroupableUndoDocument)editor.getDocument()).endCompoundEdit();
        }
        catch(BadLocationException e)
        {
            assert false : e.getMessage();
        }
    }
    
    /**blocking */
    void copyAsHtml()
    {
        var htmlbuf = new StringBuilder();
        htmlbuf.append("<pre>");
        editor.getText().chars().forEach((c) -> 
        {
            htmlbuf.append(HtmlEscaper.nonAscii(c));
        });
        htmlbuf.append("</pre>");
        htmlbuf.append(getResultSetTableModel().toHtml());
        var selection = new HtmlSelection(htmlbuf.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection,null);
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
    
    /**
     * @return the number of lines read
     */
    int load(LineNumberReader r)
    throws IOException
    {
        var newText = new StringBuilder(editor.getText());
        String line=null;
        int linesRead = 0;
        while((line=r.readLine())!=null)
        {
            linesRead++;
            if(line.equals("--CSV Result"))
            {
                var rsm = new ResultSetTableModel();
                rsm.load(r);
                addResultSetTable(rsm);
                break;
            }
            else
            {
                if(newText.length()>0) newText.append("\n");
                newText.append(line);
            }
        }
        var document = (AbstractDocument)editor.getDocument();
        for(var l : document.getUndoableEditListeners())
        {
            document.removeUndoableEditListener(l);
        }
        editor.setText(newText.toString());
        undoManagerProxy = new UndoManagerProxy(editor);
        return linesRead;
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
            fireBufferEvent(Type.DRY_FETCH);
            return;
        }
        
        if(split && resultview!=null) try
        {
            int end = savedSelectionEnd;
            Document d = editor.getDocument();

            var e = new JdbcBufferControllerEvent(this,Type.SPLIT_REQUESTED);
            e.text = d.getText(0,end);
            e.selectionStart = savedSelectionStart;
            e.selectionEnd = end;
            fireBufferEvent(e);
            
            /* Use Document API so that the editor does not request a viewport
             * change. */
            if(d.getLength()>end && d.getText(end,1).equals("\n")) end++;
            editor.getDocument().remove(0,end);

            savedCaretPosition = savedSelectionStart = savedSelectionEnd = 0;
        }
        catch(BadLocationException e)
        {
            assert false : e.getMessage();
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
        editor.setSelectionEnd(savedCaretPosition);
        editor.setSelectionStart(savedCaretPosition);
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
                (int)Math.min(resultviewHeight,preferredSize.getHeight())));
        
        resultview.setCellSelectionEnabled(true);
        
        resultview.addKeyListener(resultsetKeyListener);
        
        var resultscrollpane = new JScrollPane(resultview,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        var resultviewConstraints = new GridBagConstraints();
        resultviewConstraints.anchor = GridBagConstraints.WEST;
        resultviewConstraints.gridy = 1;
        
        /* https://bugs.openjdk.java.net/browse/JDK-4890196 */
        var newMl = new ResultPaneMouseWheelListener();
        newMl.originalListener = resultscrollpane.getMouseWheelListeners()[0];
        resultscrollpane.removeMouseWheelListener(newMl.originalListener);
        resultscrollpane.addMouseWheelListener(newMl);
        
        panel.add(resultscrollpane,resultviewConstraints);
        panel.revalidate();
        
        fireBufferEvent(Type.RESULT_VIEW_UPDATED);
    }
    
    void addResultSetPopup()
    {
        var popup = new JPopupMenu();
        JMenuItem item;
        item = new JMenuItem("Copy as HTML",KeyEvent.VK_H);
        item.addActionListener((e) -> copyAsHtml());
        popup.add(item);
        item = new JMenuItem("Close",KeyEvent.VK_C);
        item.addActionListener((e) ->
        {
            closeCurrentResultSet();
            resultview=null;
            panel.remove(1);
            panel.revalidate();
            fireBufferEvent(Type.RESULT_VIEW_CLOSED);
        });
        popup.add(item);
        var popuplistener = new MouseAdapter()
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
        };
        resultview.addMouseListener(popuplistener);
        resultview.getTableHeader().addMouseListener(popuplistener);
    }
    
    class ResultPaneMouseWheelListener implements MouseWheelListener
    {
        MouseWheelListener originalListener;

        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            int wr = e.getWheelRotation();
            JViewport vp = (JViewport)resultview.getParent();
            if(wr > 0)
            {
                if(vp.getViewPosition().getY() + vp.getHeight() >= 
                        resultview.getHeight())
                {
                    fireBufferEvent(Type.SCROLLED_SOUTH);
                }
                else
                {
                    originalListener.mouseWheelMoved(e);
                }
            }
            else if(wr < 0)
            {
                if(vp.getViewPosition().getY() == 0)
                {
                    fireBufferEvent(Type.SCROLLED_NORTH);
                }
                else
                {
                    originalListener.mouseWheelMoved(e);
                }
            }
        }
    };
    
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
            var e = new JdbcBufferControllerEvent(JdbcBufferController.this,
                    Type.SELECTED_RECT_CHANGED);
            e.selectedRect = new Rectangle(
                    (int)cellRect.getX(),
                    (int)(rect.getHeight() + 
                          cellRect.getY() - 
                          position.getY() +
                          headerBounds.getHeight()),
                    (int)cellRect.getWidth(),
                    (int)cellRect.getHeight()
            );
            fireBufferEvent(e);
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
