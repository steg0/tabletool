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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringWriter;
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

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.border.Border;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;

import de.steg0.deskapps.tabletool.JdbcBufferEvent.Type;

class JdbcBufferController
{
    private static final MessageFormat FETCH_LOG_FORMAT = 
            new MessageFormat("{0} row{0,choice,0#s|1#|1<s} fetched from {4} in {1} ms and ResultSet {2} at {3}\n");
    private static final MessageFormat FETCH_ALL_LOG_FORMAT = 
            new MessageFormat("{0,choice,0#All 0 rows|1#The only row|1<All {0} rows} fetched from {4} in {1} ms and ResultSet {2} at {3}\n");
    private static final MessageFormat UPDATE_LOG_FORMAT = 
            new MessageFormat("{0,choice,-1#0 rows|0#0 rows|1#1 row|1<{0} rows} affected in {1} ms at {2}\n");

    private static final Pattern QUERYPATTERN = Pattern.compile(
            "^(?:[^\\;\\-\\']*\\'[^\\']*\\'|[^\\;\\-\\']*\\-\\-[^\\n]*\\n|[^\\;\\-\\']*\\-(?!\\-))*[^\\;\\-\\']*(?:\\;|$)");
    
    interface Listener extends EventListener
    {
        void bufferActionPerformed(JdbcBufferEvent e);
    }
    
    Logger logger = Logger.getLogger("tabletool.editor");

    private final JFrame cellDisplay,infoDisplay;

    JPanel panel = new JPanel(new GridBagLayout());
    
    JTextArea editor = new JTextArea(new GroupableUndoDocument());
    private KeyListener editorKeyListener =
            new JdbcBufferEditorKeyListener(this);
    WordSelectAdapter selectListener = new WordSelectAdapter(editor);
    private JdbcBufferDocumentListener documentListener = 
            new JdbcBufferDocumentListener(this);
    boolean isUnsaved() { return documentListener.unsaved; }
    private Border unfocusedBorder = BorderFactory.createDashedBorder(Color.WHITE);
    void setSaved()
    {
        documentListener.unsaved = false;
        if(!editor.hasFocus()) editor.setBorder(unfocusedBorder);
    }
    
    /**The system-default editor background */
    final Color defaultBackground = editor.getBackground();
    
    UndoManager undoManager = new UndoManager();
    
    {
        editor.getDocument().addUndoableEditListener(undoManager);
        Border focusedBorder = BorderFactory.createDashedBorder(Color.BLUE);
        Border unsavedBorder = BorderFactory.createDashedBorder(Color.GRAY);
        editor.setBorder(unfocusedBorder);
        editor.addFocusListener(new FocusListener()
        {
            @Override public void focusGained(FocusEvent e)
            {
                editor.setBorder(focusedBorder);
            }
            @Override public void focusLost(FocusEvent e)
            {
                editor.setBorder(isUnsaved()?unsavedBorder:unfocusedBorder);
            }
        });
    }
    
    JTable resultview;
    JdbcBufferConfigSource configSource;
    
    Consumer<String> log;
    
    JdbcBufferController(JFrame cellDisplay,JFrame infoDisplay,
            Consumer<String> updateLog,JdbcBufferConfigSource configSource,
            Listener listener)
    {
        this.cellDisplay = cellDisplay;
        this.infoDisplay = infoDisplay;
        this.configSource = configSource;
        this.listener = listener;
        this.log = updateLog;
        
        var editorConstraints = new GridBagConstraints();
        editorConstraints.anchor = GridBagConstraints.WEST;
        panel.add(editor,editorConstraints);
        panel.setBackground(defaultBackground);
        
        editor.addKeyListener(editorKeyListener);
        if(configSource.getEditorFontName() != null)
        {
            Font f = editor.getFont(),f2=new Font(
                    configSource.getEditorFontName(),f.getStyle(),
                    f.getSize());
            editor.setFont(f2);
        }
        editor.setTabSize(configSource.getEditorTabsize());
        
        var actions = new JdbcBufferActions(this);
        var im = editor.getInputMap();
        im.put(getKeyStroke(KeyEvent.VK_ENTER,CTRL_MASK),"Execute");
        im.put(getKeyStroke(KeyEvent.VK_ENTER,CTRL_MASK|SHIFT_MASK),
                "Execute/Split");
        im.put(getKeyStroke(KeyEvent.VK_F1,0),"Show Info");
        im.put(getKeyStroke(KeyEvent.VK_F2,0),"Show Snippets");
        im.put(getKeyStroke(KeyEvent.VK_F8,0),"Show Completions");
        im.put(getKeyStroke(KeyEvent.VK_SLASH,CTRL_MASK),"Toggle Comment");
        im.put(getKeyStroke(KeyEvent.VK_Z,CTRL_MASK),"Undo");
        im.put(getKeyStroke(KeyEvent.VK_Y,CTRL_MASK),"Redo");
        var am = editor.getActionMap();
        am.put("Execute",actions.executeAction);
        am.put("Execute/Split",actions.executeSplitAction);
        am.put("Show Info",actions.showInfoAction);
        am.put("Show Snippets",actions.showSnippetsPopupAction);
        am.put("Show Completions",actions.showCompletionPopupAction);
        am.put("Toggle Comment",actions.toggleCommentAction);
        am.put("Undo",actions.undoAction);
        am.put("Redo",actions.redoAction);
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
        setResultViewFontSize(resultview,newSize);
    }

    int searchNext(int loc,String text)
    {
        int index = editor.getText().toLowerCase().indexOf(
                text.toLowerCase(),loc);
        if(index>=0)
        {
            logger.log(Level.FINE,"Found at location: {0}",index);
            editor.requestFocusInWindow();
            editor.setSelectionStart(index);
            logger.log(Level.FINE,"Setting selection end to {0}",text.length());
            editor.setSelectionEnd(index+text.length());
        }
        return index;
    }
    
    private void setResultViewFontSize(JTable resultview,int newSize)
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
                (int)Math.min(configSource.getResultViewHeight()*lineHeight,
                        preferredSize.getHeight()));
        logger.log(Level.FINE,"Sizing table, viewportSize={0}, "+
                "lineHeight={1}",new Object[]{viewportSize,lineHeight});
        resultview.setPreferredScrollableViewportSize(viewportSize);
    }
    
    private static JViewport findViewportParent(Component c)
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
    
    private void setCaretPositionInLine(int position)
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

    private final Listener listener;
    
    void fireBufferEvent(JdbcBufferEvent e)
    {
        listener.bufferActionPerformed(e);
    }

    void fireBufferEvent(Type type)
    {
        fireBufferEvent(new JdbcBufferEvent(this,type));
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
            if(editor.getText().length()>0)
            {
                editor.getDocument().insertString(0,"\n",null);
            }
            editor.getDocument().insertString(0,c.editor.getText(),null);
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
    
    private void openAsHtml(boolean transposed)
    {
        try(var exporter = new HtmlExporter())
        {
            var htmlbuf = new StringBuilder();
            htmlbuf.append("<pre>");
            editor.getText().chars().forEach((c) -> 
            {
                htmlbuf.append(HtmlEscaper.nonAscii(c));
            });
            htmlbuf.append("</pre>");
            exporter.getWriter().write(htmlbuf.toString());                
            if(transposed)
            {
                getResultSetTableModel().toHtmlTransposed(exporter.getWriter());
            }
            else
            {
                exporter.getWriter().write(getResultSetTableModel().toHtml());
            }
            exporter.openWithDesktop();
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(
                    cellDisplay,
                    "Error exporting to file: "+e.getMessage(),
                    "Error exporting",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openAsCsv()
    {
        var sw = new StringWriter();
        try
        {
            getResultSetTableModel().store(sw,false);
        }
        catch(IOException e)
        {
            assert false: e.getMessage();
        }
        CsvExporter.openTemp(cellDisplay,sw.toString());
    }
    
    void store(Writer w)
    throws IOException
    {
        w.write(editor.getText());
        var rsm = getResultSetTableModel();
        if(rsm != null)
        {
            w.write('\n');
            w.write("--CSV Result");
            if(resultSetMessage!=null && !resultSetMessage.isEmpty())
            {
                w.write(" ");
                w.write(ResultSetTableModel.sanitizeForCsv(
                        resultSetMessage,true));
            }
            w.write("\n");
            rsm.store(w,true);
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
            if(line.startsWith("--CSV Result"))
            {
                var lmr = new CsvResultLogMessageReader();
                lmr.load(line.substring(12),r);
                resultSetMessage = lmr.message;
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
    
    private int savedCaretPosition,savedSelectionStart,savedSelectionEnd;

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
        
        if(split) try
        {
            int end = savedSelectionEnd;
            Document d = editor.getDocument();

            var e = new JdbcBufferEvent(this,Type.SPLIT_REQUESTED);
            e.text = d.getText(0,end);
            e.selectionStart = savedSelectionStart;
            e.selectionEnd = end;
            fireBufferEvent(e);
            
            /* Split now so that the user cannot edit anything inbetween,
             * which would mess up our offsets. Use Document API so that
             * the editor does not request a viewport change. */
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

    private ResultSetTableModel getResultSetTableModel()
    {
        if(resultview != null)
        {
            return (ResultSetTableModel)resultview.getModel();
        }
        return null;
    }
    
    private String selectCurrentQuery()
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
    
    private void restoreCaretPosition()
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
    
    /**
     * The log message associated with the last fetch operation. Empty
     * or <code>null</code> means that no message is available, either because
     * no result is available, or one was loaded back from a file that didn't
     * carry a message.
     */
    String resultSetMessage;

    /**
     * The first argument is the result data; <code>null</code> means there is
     * nothing to display, which can lead to the buffer being closed.
     */
    private BiConsumer<ResultSetTableModel,Long> resultConsumer = (rsm,t) ->
    {
        /* In case of a possible fresh split, close again if there is
         * nothing to display */
        if(rsm==null)
        {
            if(resultview==null) closeBuffer();
            return;
        }

        restoreCaretPosition();

        Object[] logargs = {
                rsm.getRowCount(),
                t,
                rsm.resultSetClosed? "closed" : "open",
                new Date().toString(),
                rsm.connectionDescription
        };
        resultSetMessage = rsm.getRowCount() < rsm.fetchsize?
                FETCH_ALL_LOG_FORMAT.format(logargs) :
                FETCH_LOG_FORMAT.format(logargs);
        log.accept(resultSetMessage);
        
        addResultSetTable(rsm);
    };

    private KeyListener resultsetKeyListener = 
        new JdbcBufferResultSetKeyListener(this);

    private void addResultSetTable(ResultSetTableModel rsm)
    {
        if(panel.getComponentCount()==2) panel.remove(1);

        resultview = new JTable(rsm);
        if(resultSetMessage!=null && !resultSetMessage.isEmpty())
        {
            logger.log(Level.FINE,"resultSetMessage={0}",resultSetMessage);
            resultview.setToolTipText(resultSetMessage);
        }
        setResultViewFontSize(resultview,editor.getFont().getSize());
        
        new CellDisplayController(cellDisplay,resultview,log,configSource.pwd);
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
        var newMl = new JdbcBufferResultPaneMouseWheelListener(this);
        newMl.originalListener = resultscrollpane.getMouseWheelListeners()[0];
        resultscrollpane.removeMouseWheelListener(newMl.originalListener);
        resultscrollpane.addMouseWheelListener(newMl);
        
        panel.add(resultscrollpane,resultviewConstraints);
        panel.revalidate();
        
        fireBufferEvent(Type.RESULT_VIEW_UPDATED);
    }

    BiConsumer<ResultSetTableModel,Long> infoResultConsumer = (rsm,t) ->
    {
        if(rsm==null||rsm.getRowCount()==0) return;

        showInfoTable(rsm);
        
        Object[] logargs = {
                rsm.getRowCount(),
                t,
                rsm.resultSetClosed? "closed" : "open",
                new Date().toString(),
                rsm.connectionDescription
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
    
    private void showInfoTable(ResultSetTableModel rsm)
    {
        JTable inforesultview = new JTable(rsm);
        setResultViewFontSize(inforesultview,editor.getFont().getSize());
        inforesultview.setCellSelectionEnabled(true);
        new InfoDisplayController(infoDisplay,inforesultview);
    }

    private void closeBuffer()
    {
        closeCurrentResultSet();
        resultview=null;
        resultSetMessage=null;
        if(panel.getComponentCount()>1)
        {
            panel.remove(1);
            panel.revalidate();
        }
        fireBufferEvent(Type.RESULT_VIEW_CLOSED);
    }
    
    private void addResultSetPopup()
    {
        var popup = new JPopupMenu();
        JMenuItem item;
        item = new JMenuItem("Open as HTML",KeyEvent.VK_H);
        item.addActionListener((e) -> openAsHtml(false));
        popup.add(item);
        item = new JMenuItem("Open as HTML (transposed)",KeyEvent.VK_T);
        item.addActionListener((e) -> openAsHtml(true));
        popup.add(item);
        item = new JMenuItem("Open as CSV",KeyEvent.VK_V);
        item.addActionListener((e) -> openAsCsv());
        popup.add(item);
        item = new JMenuItem("Close",KeyEvent.VK_C);
        item.addActionListener((e) -> closeBuffer());
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
}