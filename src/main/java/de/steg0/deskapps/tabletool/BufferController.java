package de.steg0.deskapps.tabletool;

import static java.awt.event.ActionEvent.ALT_MASK;
import static java.awt.event.ActionEvent.CTRL_MASK;
import static java.lang.Math.max;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Date;
import java.util.EventListener;
import java.util.Objects;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.border.Border;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;

import de.steg0.deskapps.tabletool.BufferEvent.Type;
import de.steg0.deskapps.tabletool.PlaceholderInputController.SubstitutionCanceledException;

class BufferController
{
    static final String CONNECT_COMMENT = "-- connect ";
    
    private static final String CONNECTION_LABEL_PREFIX =
            "\u00b7\u00b7\u00b7\u00b7 ";
    private static final String CONNECTION_LABEL_SUFFIX = " ";

    private static final MessageFormat FETCH_LOG_FORMAT = 
            new MessageFormat("{0} row{0,choice,0#s|1#|1<s} fetched from {4} in {1} ms and ResultSet {2} at {3}");
    private static final MessageFormat FETCH_ALL_LOG_FORMAT = 
            new MessageFormat("{0,choice,0#All 0 rows|1#The only row|1<All {0} rows} fetched from {4} in {1} ms and ResultSet {2} at {3}");

    private static final Pattern QUERYPATTERN = Pattern.compile(
            "^(?:[^\\;\\-\\']*\\'[^\\']*\\'|[^\\;\\-\\']*\\-\\-[^\\n]*\\n|[^\\;\\-\\']*\\-(?!\\-))*[^\\;\\-\\']*(?:\\;|$)");
    
    interface Listener extends EventListener
    {
        void bufferActionPerformed(BufferEvent e);
    }
    
    Logger logger = Logger.getLogger("tabtype");

    private final JFrame parent,cellDisplay,infoDisplay;

    JPanel panel = new JPanel(new GridBagLayout());
    
    JTextArea editor = new JTextArea(new GroupableUndoDocument());
    private KeyListener editorKeyListener =
            new BufferEditorKeyListener(this);
    WordSelectAdapter selectListener = new WordSelectAdapter(editor);
    private BufferDocumentListener documentListener = 
            new BufferDocumentListener(this);
    boolean isUnsaved() { return documentListener.unsaved; }
    private final Border unfocusedBorder;
    private final Color unfocusedBorderColor;
    private JLabel connectionLabel = new JLabel();

    void setSaved()
    {
        documentListener.unsaved = false;
        if(!editor.hasFocus()) {
            editor.setBorder(unfocusedBorder);
            connectionLabel.setForeground(unfocusedBorderColor);
        }
    }
    
    /**The system-default editor background */
    final Color defaultBackground = editor.getBackground();
    
    UndoManager undoManager = new UndoManager();
    
    {
        editor.getDocument().addUndoableEditListener(undoManager);
    }
    
    JTable resultview;
    JLabel resultMessageLabel;
    BufferConfigSource configSource;
    JdbcParametersInputController parametersController;
    private PlaceholderInputController placeholderInputController;
    
    Consumer<String> log;
    final BufferUpdateCountConsumer updateCountConsumer;
        
    BufferController(JFrame parent,JFrame cellDisplay,JFrame infoDisplay,
            JdbcParametersInputController parametersController,
            Consumer<String> updateLog,BufferConfigSource configSource,
            Listener listener)
    {
        this.cellDisplay = cellDisplay;
        this.infoDisplay = infoDisplay;
        this.parametersController = parametersController;
        this.parent = parent;
        this.configSource = configSource;
        this.listener = listener;
        placeholderInputController = new PlaceholderInputController(
                configSource,parent);
        this.log = updateLog;
        this.updateCountConsumer = new BufferUpdateCountConsumer(parent,this);
        
        unfocusedBorderColor = configSource.getNonFocusedEditorBorderColor();
        Color focusedBorderColor = configSource.getFocusedEditorBorderColor();
        Color unsavedBorderColor = configSource.getUnsavedEditorBorderColor();
        unfocusedBorder = BorderFactory.createDashedBorder(
                unfocusedBorderColor);
        Border focusedBorder = BorderFactory.createDashedBorder(
                focusedBorderColor);
        Border unsavedBorder = BorderFactory.createDashedBorder(
                unsavedBorderColor);
        editor.setBorder(unfocusedBorder);
        connectionLabel.setForeground(unfocusedBorderColor);
        editor.addFocusListener(new FocusListener()
        {
            @Override public void focusGained(FocusEvent e)
            {
                editor.setBorder(focusedBorder);
                connectionLabel.setForeground(focusedBorderColor);
            }
            @Override public void focusLost(FocusEvent e)
            {
                if(isUnsaved())
                {
                    editor.setBorder(isUnsaved()?
                            unsavedBorder:unfocusedBorder);
                    connectionLabel.setForeground(unsavedBorderColor);
                }
                else
                {
                    editor.setBorder(unfocusedBorder);
                    connectionLabel.setForeground(unfocusedBorderColor);
                }
            }
        });
        
        var editorConstraints = new GridBagConstraints();
        editorConstraints.anchor = GridBagConstraints.WEST;
        panel.add(editor,editorConstraints);
        var connectionLabelConstraints = new GridBagConstraints();
        connectionLabelConstraints.anchor = GridBagConstraints.NORTHWEST;
        setConnectionLabelFontSize();
        panel.add(connectionLabel,connectionLabelConstraints);
        panel.setBackground(defaultBackground);
        
        editor.addKeyListener(editorKeyListener);
        String fontName = configSource.getEditorFontName();
        if(fontName != null)
        {
            Font f = editor.getFont();
            Font f2=new Font(fontName,f.getStyle(),f.getSize());
            editor.setFont(f2);
        }
        Integer fontSize = configSource.getEditorFontSize();
        if(configSource.getEditorFontSize() != null)
        {
            Font f = editor.getFont();
            Font f2=new Font(f.getFontName(),f.getStyle(),fontSize);
            editor.setFont(f2);
        }
        editor.setTabSize(configSource.getEditorTabsize());
        
        var actions = new BufferActions(parent,this);
        var im = editor.getInputMap();
        im.put(getKeyStroke(KeyEvent.VK_F5,0),"Execute");
        im.put(getKeyStroke(KeyEvent.VK_R,CTRL_MASK),"Execute");
        im.put(getKeyStroke(KeyEvent.VK_ENTER,ALT_MASK),"JDBC Parameters");
        im.put(getKeyStroke(KeyEvent.VK_ENTER,CTRL_MASK),"Execute/Split");
        im.put(getKeyStroke(KeyEvent.VK_F1,0),"Show Info");
        im.put(getKeyStroke(KeyEvent.VK_F2,0),"Show Snippets");
        im.put(getKeyStroke(KeyEvent.VK_F8,0),"Show Completions");
        im.put(getKeyStroke(KeyEvent.VK_SLASH,CTRL_MASK),"Toggle Comment");
        im.put(getKeyStroke(KeyEvent.VK_Z,CTRL_MASK),"Undo");
        im.put(getKeyStroke(KeyEvent.VK_Y,CTRL_MASK),"Redo");
        im.put(getKeyStroke(KeyEvent.VK_G,CTRL_MASK),"Go To Line");
        var am = editor.getActionMap();
        am.put("Execute",actions.executeAction);
        am.put("JDBC Parameters",actions.showJdbcParametersAction);
        am.put("Execute/Split",actions.executeSplitAction);
        am.put("Show Info",actions.showInfoAction);
        am.put("Show Snippets",actions.showSnippetsPopupAction);
        am.put("Show Completions",actions.showCompletionPopupAction);
        am.put("Toggle Comment",new EditorPrefixToggler(editor,"--"));
        am.put("Undo",actions.undoAction);
        am.put("Redo",actions.redoAction);
        am.put("Go To Line",actions.goToLineAction);
    }
    
    void setBranding(Color background,String text)
    {
        Objects.requireNonNull(text);
        editor.setBackground(background);
        panel.setBackground(background);
        if(text.isBlank()) connectionLabel.setText(text);
        else connectionLabel.setText(CONNECTION_LABEL_PREFIX+text+
                CONNECTION_LABEL_SUFFIX);
        TableColorizer.colorize(resultview,getBrandingBackground());
    }

    Color getBrandingBackground()
    {
        return editor.getBackground();
    }

    String getBrandingText()
    {
        String label = connectionLabel.getText();
        if(label.isBlank()) return label;
        return label.substring(CONNECTION_LABEL_PREFIX.length(),
                label.length()-CONNECTION_LABEL_SUFFIX.length());
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
        TableFontSizer.setFontSize(resultview,newSize,
                configSource.getResultViewHeight());
        setResultSetMessageLabelFontSize();
        setConnectionLabelFontSize();
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
    
    private void setResultSetMessageLabelFontSize()
    {
        if(resultMessageLabel==null) return;
        Font resultMessageFont = new Font(
                resultview.getFont().getName(),
                Font.ITALIC,
                (int)(resultview.getFont().getSize() * .9)
        );
        resultMessageLabel.setFont(resultMessageFont);
    }

    private void setConnectionLabelFontSize()
    {
        Font connectionLabelFont = new Font(
                connectionLabel.getFont().getName(),
                Font.ITALIC,
                (int)(editor.getFont().getSize() * .9)
        );
        connectionLabel.setFont(connectionLabelFont);
    }
    
    private static JViewport findViewportParent(Component c)
    {
        if(c==null) return null;
        if(c instanceof JViewport v) return v;
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

    String getTextFromCurrentLine(boolean endWithCaret)
    {
        String t = editor.getText();
        int caret = editor.getCaretPosition();
        logger.log(Level.FINE,"caret={0}",caret);
        int index = t.lastIndexOf('\n',caret-1)+1;
        return endWithCaret? t.substring(index,caret) : t.substring(index);
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
        editor.setCaretPosition(max(0,i));
        setCaretPositionInLine(offset);
    }

    private final Listener listener;
    
    void fireBufferEvent(BufferEvent e)
    {
        listener.bufferActionPerformed(e);
    }

    void fireBufferEvent(Type type)
    {
        fireBufferEvent(new BufferEvent(this,type));
    }

    /**
     * @param characterX
     *            the X position to set the caret to, which is a character
     *            position relative to the start of the line that has the caret.
     * @param pointY
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
            int position=max(0,editor.viewToModel2D(p));
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
    
    void append(String text)
    {
        Document d = editor.getDocument();
        try
        {
            int caret = editor.getCaretPosition();
            if(d.getLength()>0 && !text.startsWith("\n"))
            {
                d.insertString(d.getLength(),"\n",null);
            }
            d.insertString(d.getLength(),text,null);
            editor.setCaretPosition(caret);
        }
        catch(BadLocationException e)
        {
            assert false : e.getMessage();
        }
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
            if(rsm.resultMessage!=null && !rsm.resultMessage.isEmpty())
            {
                w.write(" ");
                w.write(ResultSetTableModel.sanitizeForCsv(
                        rsm.resultMessage,true));
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
                var rsm = new ResultSetTableModel();
                rsm.resultMessage = lmr.message;
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
        if(getTextFromCurrentLine(false).startsWith(CONNECT_COMMENT))
        {
            logger.log(Level.FINE,"Found connect comment");
            fireBufferEvent(Type.DRY_FETCH);
            return;
        }
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

        String placeholderlog=null;
        try
        {
            text=placeholderInputController.fill(text);
            placeholderlog=placeholderInputController.describeLastValues();
        }
        catch(SubstitutionCanceledException e)
        {
            log.accept("Substitution canceled at "+new Date());
            return;
        }
        
        if(split) try
        {
            int end = savedSelectionEnd;
            logger.log(Level.FINE,"Cutting to new buffer at {0}",end);
            Document d = editor.getDocument();
            var e = new BufferEvent(this,Type.SPLIT_REQUESTED);
            int len = d.getLength()-end;
            logger.log(Level.FINE,"Total length {0}",len);
            e.removedRsm = getResultSetTableModel();
            e.removedText = d.getText(end,len);
            fireBufferEvent(e);

            resultview=null;  /* this acts as a flag that we're in a split */
            removeResultView();

            /* Split now so that the user cannot edit anything inbetween,
             * which would mess up our offsets. Use Document API so that
             * the editor does not request a viewport change. */
            d.remove(end,len);
        }
        catch(BadLocationException e)
        {
            assert false : e.getMessage();
        }

        connection.submit(text,fetchsize,parametersController,placeholderlog,
                resultConsumer,updateCountConsumer,log);
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
                /* Remove block prefixes if possible, they don't make much
                 * sense for what's supported with our query detection */
                Matcher blockMatcher = CallableStatementMatchers
                        .blockPrefixMatch(match);
                int prefixLen = match.length()-blockMatcher.group(2).length();
                editor.select(
                        offset + m.start() + prefixLen,
                        offset + m.end());

                return match.substring(prefixLen);
            }
            text = text.substring(match.length());
            position -= match.length();
            offset += match.length();
            m = QUERYPATTERN.matcher(text);
        }
        return null;
    }
    
    /**
     * Removes selection highlight when a query has returned.
     * 
     * @param force If true, does not check whether the selection was
     * moved inbetween. This is used after failed split-fetch where
     * the undo operation messes with the selection.
     */
    void restoreCaretPosition(boolean force)
    {
        if(!force && (editor.getSelectionStart()!=savedSelectionStart ||
           editor.getSelectionEnd()!=savedSelectionEnd)) return;
        editor.setSelectionEnd(savedCaretPosition);
        editor.setSelectionStart(savedCaretPosition);
        editor.setCaretPosition(savedCaretPosition);
    }

    /**
     * The first argument is the result data; <code>null</code> means there is
     * nothing to display, which can lead to the buffer being closed.
     */
    private BiConsumer<ResultSetTableModel,Long> resultConsumer = (rsm,t) ->
    {
        if(rsm==null)
        {
            fireBufferEvent(Type.NULL_FETCH);
            return;
        }

        restoreCaretPosition(false);

        Object[] logargs = {
                rsm.getRowCount(),
                t,
                rsm.resultSetClosed? "closed" : "open",
                rsm.date.toString(),
                rsm.connectionDescription
        };
        String paramlog = (rsm.inlog + rsm.outlog).trim();
        if(!paramlog.isEmpty()) paramlog = " - " + paramlog;
        if(!rsm.placeholderlog.isEmpty()) paramlog += " - " +
                rsm.placeholderlog;

        if(rsm.getRowCount() < rsm.fetchsize)
        {
            log.accept(FETCH_ALL_LOG_FORMAT.format(logargs) + paramlog);
        }
        else
        {
            log.accept(FETCH_LOG_FORMAT.format(logargs) + paramlog);
        }
        
        addResultSetTable(rsm);

        fireBufferEvent(Type.RESULT_VIEW_UPDATED);
    };

    private KeyListener resultsetKeyListener = 
        new BufferResultSetKeyListener(this);

    void addResultSetTable(ResultSetTableModel rsm)
    {
        removeResultView();

        resultview = new JTable(rsm);
        
        new CellDisplayController(cellDisplay,resultview,log,configSource.pwd);
        new BufferResultSetPopup(parent,this).attach();
        
        resultview.setCellSelectionEnabled(true);
        
        resultview.addKeyListener(resultsetKeyListener);
        
        var resultscrollpane = new JScrollPane(resultview,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        resultscrollpane.setComponentOrientation(
                ComponentOrientation.RIGHT_TO_LEFT);
        TableColorizer.colorize(resultview,getBrandingBackground());

        var resultviewConstraints = new GridBagConstraints();
        resultviewConstraints.anchor = GridBagConstraints.WEST;
        resultviewConstraints.gridwidth = 2;
        resultviewConstraints.gridy = 1;
        
        /* https://bugs.openjdk.java.net/browse/JDK-4890196 */
        var newMl = new BufferResultPaneMouseWheelListener(this);
        newMl.originalListener = resultscrollpane.getMouseWheelListeners()[0];
        resultscrollpane.removeMouseWheelListener(newMl.originalListener);
        resultscrollpane.addMouseWheelListener(newMl);
        
        panel.add(resultscrollpane,resultviewConstraints);

        if(rsm.resultMessage!=null && !rsm.resultMessage.isEmpty())
        {
            logger.log(Level.FINE,"resultMessage={0}",rsm.resultMessage);
            resultMessageLabel = new JLabel(rsm.resultMessage);
            resultMessageLabel.setForeground(Color.GRAY);
            var resultSetMessageConstraints = new GridBagConstraints();
            resultSetMessageConstraints.anchor = GridBagConstraints.WEST;
            resultSetMessageConstraints.gridwidth = 2;
            resultSetMessageConstraints.gridy = 2;
            panel.add(resultMessageLabel,resultSetMessageConstraints);
        }

        TableFontSizer.setFontSize(resultview,editor.getFont().getSize(),
                configSource.getResultViewHeight());
        setResultSetMessageLabelFontSize();

        panel.revalidate();
    }

    BiConsumer<ResultSetTableModel,Long> infoResultConsumer = (rsm,t) ->
    {
        if(rsm==null||rsm.getRowCount()==0)
        {
            log.accept("No result available at "+new Date());
            return;
        }

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
        TableFontSizer.setFontSize(inforesultview,sizes.isEmpty()?
                editor.getFont().getSize() : sizes.get(0),
                configSource.getResultViewHeight());
        inforesultview.setCellSelectionEnabled(true);
        inforesultview.setAutoCreateRowSorter(true);
        inforesultview.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        new InfoDisplayController(infoDisplay,inforesultview);
    }

    private void removeResultView()
    {
        while(panel.getComponentCount()>2) panel.remove(2);
    }

    void closeBuffer()
    {
        closeCurrentResultSet();
        resultview=null;
        resultMessageLabel=null;
        removeResultView();
        fireBufferEvent(Type.RESULT_VIEW_CLOSED);
    }
}