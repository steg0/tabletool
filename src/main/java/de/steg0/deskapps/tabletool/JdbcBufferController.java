package de.steg0.deskapps.tabletool;

import static java.awt.event.ActionEvent.CTRL_MASK;
import static java.awt.event.ActionEvent.SHIFT_MASK;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Date;
import java.util.EventListener;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;

import de.steg0.deskapps.tabletool.JdbcBufferControllerEvent.Type;

class JdbcBufferController
{
    static final MessageFormat FETCH_LOG_FORMAT = 
            new MessageFormat("{0} row{0,choice,0#s|1#|1<s} fetched in {1} ms and ResultSet {2} at {3}\n");
    static final MessageFormat FETCH_ALL_LOG_FORMAT = 
            new MessageFormat("{0,choice,0#All 0 rows|1#The only row|1<All {0} rows} fetched in {1} ms and ResultSet {2} at {3}\n");
    static final MessageFormat UPDATE_LOG_FORMAT = 
            new MessageFormat("{0,choice,-1#0 rows|0#0 rows|1#1 row|1<{0} rows} affected in {1} ms at {2}\n");

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
    WordSelectListener selectListener = new WordSelectListener(editor);
    
    /**The system-default editor background */
    final Color defaultBackground = editor.getBackground();
    
    UndoManager undoManager = new UndoManager();
    {
        editor.getDocument().addUndoableEditListener(undoManager);
    }
    
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
        
        var im = editor.getInputMap();
        im.put(getKeyStroke(KeyEvent.VK_ENTER,CTRL_MASK),"Execute");
        im.put(getKeyStroke(KeyEvent.VK_ENTER,CTRL_MASK|SHIFT_MASK),
                "Execute/Split");
        im.put(getKeyStroke(KeyEvent.VK_SLASH,CTRL_MASK),"Toggle Comment");
        im.put(getKeyStroke(KeyEvent.VK_Z,CTRL_MASK),"Undo");
        im.put(getKeyStroke(KeyEvent.VK_Y,CTRL_MASK),"Redo");
        var am = editor.getActionMap();
        am.put("Execute",executeAction);
        am.put("Execute/Split",executeSplitAction);
        am.put("Toggle Comment",toggleCommentAction);
        am.put("Undo",undoAction);
        am.put("Redo",redoAction);
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

    Stack<Integer> sizes=new Stack<>();
    
    void zoom(double factor)
    {
        int currentSize = editor.getFont().getSize(),
            newSize;
        if(!sizes.isEmpty() &&(
                (double)currentSize/sizes.peek() < 1 && factor > 1 ||
                (double)currentSize/sizes.peek() > 1 && factor < 1)
        )
        {
            newSize = sizes.pop();
        }
        else
        {
            sizes.push(currentSize); /* assuming all elements have the same */
            newSize = (int)(currentSize * factor); /* default size in Swing */

        }
        Font f = editor.getFont(),f2=new Font(f.getName(),f.getStyle(),newSize);
        editor.setFont(f2);
        setResultViewFontSize(newSize);
    }
    
    void setResultViewFontSize(int newSize)
    {
        if(resultview==null) return;
        Font rf = resultview.getFont(),
             rf2 = new Font(rf.getName(),rf.getStyle(),newSize);
        resultview.setFont(rf2);
        var header = resultview.getTableHeader();
        Font hf = header.getFont(),
             hf2 = new Font(hf.getName(),hf.getStyle(),newSize);
        resultview.getTableHeader().setFont(hf2);
        int lineHeight = (int)hf2.getMaxCharBounds(new FontRenderContext(
                null,false,false)).getHeight();
        TableSizer.sizeColumns(resultview);
        resultview.setRowHeight(lineHeight);
        Dimension preferredSize = resultview.getPreferredSize();
        var viewportSize = new Dimension((int)preferredSize.getWidth(),
                (int)Math.min(resultviewHeight,preferredSize.getHeight()));
        logger.log(Level.FINE,"Sizing table, viewportSize={0}, "+
                "lineHeight={1}",new Object[]{viewportSize,lineHeight});
        resultview.setPreferredScrollableViewportSize(viewportSize);
    }
    
    Action
        executeAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                fetch(false);
            }
        },
        executeSplitAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                fetch(true);
            }
        },
        toggleCommentAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                togglePrefix("--",null);
            }
        },
        undoAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                if(undoManager.canUndo()) undoManager.undo();
            }
        },
        redoAction = new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                if(undoManager.canRedo()) undoManager.redo();
            }
        };

    
    KeyListener editorKeyListener = new KeyListener()
    {
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
                case KeyEvent.VK_TAB:
                    if(editor.getSelectionEnd()!=editor.getSelectionStart())
                    {
                        togglePrefix("\t",!event.isShiftDown());
                        event.consume();
                    }
                }
            }            
            catch(BadLocationException e)
            {
                assert false : e.getMessage();
            }
        }
        @Override public void keyReleased(KeyEvent e) { }
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
 
    void startLineSelection(int y)
    {
        logger.log(Level.FINE,"startLineSelection,y1={0}",y);
        selectListener.clickPos = editor.viewToModel2D(new Point(0,y));
        selectListener.clickCount = 1;
    }
    
    void dragLineSelection(int y1,int y2)
    {
        logger.log(Level.FINE,"dragLineSelection,y1={0}",y1);
        logger.log(Level.FINE,"dragLineSelection,y2={0}",y2);
        
        focusEditor(null,null);
        
        int clickPos;
        
        if(y1 >= 0)
        {
            clickPos = editor.viewToModel2D(new Point(0,y1)); 
            selectListener.clickPos = clickPos;
        }
        else try
        {
            y1=(int)editor.modelToView2D(selectListener.clickPos).getCenterY();
            logger.log(Level.FINE,"point Y from selectListener: {0}",y1);
            clickPos = editor.viewToModel2D(new Point(0,y1));
        }
        catch(BadLocationException e)
        {
            return;
        }
        
        selectListener.clickCount = 3;
        selectListener.dragPos = editor.viewToModel2D(new Point(0,y2));
        
        if(selectListener.clickPos < selectListener.dragPos)
        {
            int start = clickPos;
            int end = editor.viewToModel2D(new Point(0,y2 + getLineHeight()));
            if(end>0 && end<editor.getText().length()) end--;
            editor.select(start,end);
        }
        else
        {
            int start = selectListener.dragPos;
            int end = y1<0? clickPos :
                editor.viewToModel2D(new Point(0,y1 + getLineHeight()));
            if(end>0 && end<editor.getText().length()) end--;
            editor.setSelectionStart(start);
            editor.setSelectionEnd(end);
            editor.setCaretPosition(end);
            editor.moveCaretPosition(start);
        }
    }
    
    void prepend(JdbcBufferController c)
    {
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
                if(linesRead>1) newText.append("\n");
                newText.append(line);
            }
        }
        var document = (AbstractDocument)editor.getDocument();
        for(var l : document.getUndoableEditListeners())
        {
            document.removeUndoableEditListener(l);
        }
        editor.getDocument().removeUndoableEditListener(undoManager);
        editor.setText(newText.toString());
        undoManager = new UndoManager();
        editor.getDocument().addUndoableEditListener(undoManager);
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

    BiConsumer<Integer,Long> updateCountConsumer = (i,t) ->
    {
        Object[] logargs = {i,t,new Date().toString()};
        log.accept(UPDATE_LOG_FORMAT.format(logargs));
        restoreCaretPosition();
    };
    
    BiConsumer<ResultSetTableModel,Long> resultConsumer = (rsm,t) ->
    {
        restoreCaretPosition();

        addResultSetTable(rsm);
        
        Object[] logargs = {
                rsm.getRowCount(),
                t,
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
        setResultViewFontSize(editor.getFont().getSize());
        
        new CellDisplayController(parent,resultview,log);
        addResultSetPopup();
        
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
                if(e.isShiftDown() && vp.getViewPosition().getX() + 
                        vp.getWidth() >= resultview.getWidth())
                {
                    fireBufferEvent(Type.SCROLLED_EAST);
                }
                else if(vp.getViewPosition().getY() + vp.getHeight() >= 
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
                if(e.isShiftDown() && vp.getViewPosition().getX() == 0)
                {
                    fireBufferEvent(Type.SCROLLED_WEST);
                }
                else if(vp.getViewPosition().getY() == 0)
                {
                    fireBufferEvent(Type.SCROLLED_NORTH);
                }
                else
                {
                    originalListener.mouseWheelMoved(e);
                }
            }
        }
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
