package de.steg0.deskapps.tabletool;

import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;

/**
 * Represents a "notebook", i. e. a series of text field/result table pairs that
 * operate on a selectable {@link ConnectionWorker}.
 */
class JdbcNotebookController
{
    static final int DEFAULT_FETCH_SIZE = 10;
    
    interface Listener extends EventListener
    {
        void disconnected(ConnectionWorker connection);
        void autocommitChanged(ConnectionWorker connection,boolean enabled);
        void bufferChanged();
    }
    
    Logger logger = Logger.getLogger("tabletool.editor");
    
    private final JFrame cellDisplay,infoDisplay;
    private final PropertyHolder propertyHolder;
    private final JdbcBufferConfigSource bufferConfigSource;
    
    File file;
    
    final ConnectionListModel connections;
    private JComboBox<Connections.ConnectionState> connectionsSelector;
    Action
        focusBufferAction = new AbstractAction("Focus Buffer")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                restoreFocus();
            }
        };

    private JFormattedTextField fetchsizeField;
    
    private final JButton commitButton = new JButton("Commit");
    private final JButton rollbackButton = new JButton("Rollback");
    private final JButton disconnectButton = new JButton("Disconnect");
    private final JCheckBox autocommitCb = new JCheckBox("Autocommit",
            Connections.AUTOCOMMIT_DEFAULT);
    
    private final JScrollPane bufferPane;
    private final int scrollIncrement;
    
    private final JTextArea log = new JTextArea();
    private final JSplitPane logBufferPane;

    private void resize()
    {
        int lines = Math.min(10,log.getLineCount());
        int lineheight = log.getFontMetrics(log.getFont()).getHeight();
        int logheight = lineheight * lines;
        logger.log(Level.FINE,"logheight: {0}",logheight);
        int dividerSize = logBufferPane.getDividerSize();
        logger.log(Level.FINE,"dividerSize: {0}",dividerSize);
        int logBufferHeight = logBufferPane.getHeight();
        logger.log(Level.FINE,"logBufferHeight: {0}",logBufferHeight);
        logBufferPane.setDividerLocation(logBufferHeight - logheight - 
                dividerSize - (int)(lineheight * .4));
    }

    {
        log.setEditable(false);
        log.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { resize(); }
            @Override public void removeUpdate(DocumentEvent e) { resize(); }
            @Override public void changedUpdate(DocumentEvent e) { resize(); }
        });
    }
    
    private final Consumer<String> logConsumer;
    
    private final List<JdbcBufferController> buffers = new ArrayList<>();
    private JdbcBufferController firstBuffer() { return buffers.get(0); }
    private int lastFocusedBuffer;
    boolean hasSavedFocusPosition;
    private final JPanel bufferPanel = new JPanel(new GridBagLayout());
    final JPanel notebookPanel = new JPanel(new GridBagLayout());
    
    JdbcNotebookController(
            JFrame cellDisplay,
            JFrame infoDisplay,
            PropertyHolder propertyHolder,
            Connections connections,
            File pwd,
            Listener listener)
    {
        this.cellDisplay = cellDisplay;
        this.infoDisplay = infoDisplay;
        this.propertyHolder = propertyHolder;
        this.connections = new ConnectionListModel(connections);
        this.listener = listener;
        this.bufferConfigSource = new JdbcBufferConfigSource(propertyHolder,
                this.connections,pwd);
        
        var connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        connectionsSelector = new JComboBox<>(this.connections);
        connectionsSelector.putClientProperty("JComboBox.isTableCellEditor",
                Boolean.TRUE);
        connectionsSelector.addItemListener((e) -> updateConnection(e));
        var im = connectionsSelector.getInputMap();
        im.put(getKeyStroke(KeyEvent.VK_PAGE_DOWN,0),"Focus Buffer");
        var am = connectionsSelector.getActionMap();
        am.put("Focus Buffer",focusBufferAction);
        connectionPanel.add(connectionsSelector);
        
        connectionPanel.add(new JLabel("Fetch:"));
        
        NumberFormat format = NumberFormat.getIntegerInstance();
        NumberFormatter numberFormatter = new NumberFormatter(format);
        numberFormatter.setMinimum(1);
        fetchsizeField = new JFormattedTextField(numberFormatter);
        fetchsizeField.setColumns(5);
        fetchsizeField.setValue(DEFAULT_FETCH_SIZE);
        fetchsizeField.addPropertyChangeListener("value",fetchSizeListener);
        connectionPanel.add(fetchsizeField);
        
        commitButton.addActionListener((e) -> commit());
        connectionPanel.add(commitButton);
        
        rollbackButton.addActionListener((e) -> rollback());
        connectionPanel.add(rollbackButton);
        
        disconnectButton.addActionListener((e) -> disconnect());
        connectionPanel.add(disconnectButton);
        
        logConsumer = new JdbcNotebookLogConsumer(log);
        
        autocommitCb.addActionListener((e) -> 
        {
            onConnection((c) -> 
            {
                c.setAutoCommit(autocommitCb.isSelected(),logConsumer,() ->
                {
                    listener.autocommitChanged(c,autocommitCb.isSelected());
                });
            });
        });
        connectionPanel.add(autocommitCb);
        
        var connectionPanelConstraints = new GridBagConstraints();
        connectionPanelConstraints.anchor = GridBagConstraints.EAST;
        connectionPanelConstraints.weightx = 1;
        connectionPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        
        notebookPanel.add(connectionPanel,connectionPanelConstraints);
        
        var buffer = newBufferController();
        add(0,buffer);

        logBufferPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        logBufferPane.setResizeWeight(1);
        
        scrollIncrement = propertyHolder.getScrollIncrement();
        bufferPane = new JScrollPane(bufferPanel);
        bufferPane.getVerticalScrollBar().setUnitIncrement(scrollIncrement);
        bufferPane.getHorizontalScrollBar().setUnitIncrement(scrollIncrement);
        var ml = new BufferPaneMouseListener();
        bufferPane.addMouseListener(ml);
        bufferPane.addMouseMotionListener(ml);
        logBufferPane.add(bufferPane);
        var logPane = new JScrollPane(log);
        logBufferPane.add(logPane);

        var bufferPaneConstraints = new GridBagConstraints();
        bufferPaneConstraints.fill = GridBagConstraints.BOTH;
        bufferPaneConstraints.weighty = bufferPaneConstraints.weightx = 1;
        bufferPaneConstraints.gridy = 1;
        notebookPanel.add(logBufferPane,bufferPaneConstraints);
        
        setBackground(null);
    }

    private JdbcBufferController newBufferController()
    {
        return new JdbcBufferController(cellDisplay,infoDisplay,logConsumer,
                bufferConfigSource,bufferListener);
    }

    private void setBackground(Color bg)
    {
        if(bg==null) bg=propertyHolder.getDefaultBackground(); 
        if(bg==null) bg=firstBuffer().defaultBackground;
        bufferPanel.setBackground(bg);
        log.setBackground(bg);
        for(JdbcBufferController buffer : buffers) buffer.setBackground(bg);
    }
    
    void zoom(double factor)
    {
        for(JdbcBufferController buffer : buffers)
        {
            buffer.zoom(factor);

            if(buffer.editor.isFocusOwner())
            {
                SwingUtilities.invokeLater(() -> selectedRectChanged(buffer,
                        new Rectangle(0,0,1,buffer.getLineHeight())));
            }
        }
    }
    
    private final Listener listener;
    
    private class BufferPaneMouseListener extends MouseAdapter
    {
        int clickVpY;
        Component clickBuffer;
        
        @Override
        public void mouseClicked(MouseEvent e)
        {
            int vpY = e.getY() + (int)bufferPane.getViewport()
                    .getViewPosition().getY();
            logger.log(Level.FINE,"mouseClicked,vpY={0}",vpY);
            var component = bufferPanel.getComponentAt(new Point(0,vpY));
            for(var buffer : buffers)
            {
                if(buffer.panel == component)
                {
                    int bufferY = (int)buffer.panel.getLocation().getY();
                    int y = vpY - bufferY;
                    if(e.getClickCount() == 3)
                    {
                        buffer.dragLineSelection(y,y);
                    }
                    else if(e.isShiftDown() && buffer.editor.hasFocus())
                    {
                        buffer.dragLineSelection(-1,y);
                    }
                    else if(!e.isShiftDown())
                    {
                        buffer.focusEditor(null,y);
                        buffer.startLineSelection(y);
                    }
                    return;
                }
            }
            JdbcBufferController lastBuffer = buffers.get(buffers.size() - 1);
            if(lastBuffer.resultview != null)
            {
                exitedSouth(buffers.get(buffers.size() - 1));
            }
            else
            {
                int bufferY = (int)lastBuffer.panel.getLocation().getY();
                lastBuffer.focusEditor(null,vpY - bufferY);
            }
        }
        
        @Override
        public void mousePressed(MouseEvent e)
        {
            if(e.isShiftDown()) return;
            int bufY = (int)bufferPane.getViewport().getViewPosition().getY();
            clickVpY = bufY + e.getY();
            logger.log(Level.FINE,"mousePressed,clickVpY={0}",clickVpY);
            clickBuffer = bufferPanel.getComponentAt(new Point(0,clickVpY));
            if(!buffers.stream().anyMatch((b) -> b.panel==clickBuffer))
            {
                logger.fine("Click beyond last buffer");
                clickBuffer=buffers.get(buffers.size() - 1).panel;
            }
        }
        
        @Override
        public void mouseDragged(MouseEvent e)
        {
            int vpY = e.getY() + (int)bufferPane.getViewport()
                    .getViewPosition().getY();
            logger.log(Level.FINE,"mouseDragged,vpY={0}",vpY);
            var component = bufferPanel.getComponentAt(new Point(0,vpY));
            if(component != clickBuffer) return;
            for(var buffer : buffers)
            {
                if(buffer.panel == component)
                {
                    int bufferY = (int)buffer.panel.getLocation().getY();
                    buffer.dragLineSelection(clickVpY - bufferY,vpY - bufferY);
                    return;
                }
            }
        }
    }
    
    private final JdbcBufferController.Listener bufferListener = 
            new JdbcBufferController.Listener()
    {
        @Override
        public void bufferActionPerformed(JdbcBufferEvent e)
        {
            JdbcBufferController source = e.getSource();
            switch(e.type)
            {
            case EXITED_NORTH:
                int i=buffers.indexOf(source);
                if(i > 0) 
                {
                    JdbcBufferController target = buffers.get(i-1);
                    target.focusEditor(source.getCaretPositionInLine(),-1);
                    int h = target.getLineHeight();
                    selectedRectChanged(target,new Rectangle(0,
                            (int)target.editor.getBounds().getHeight()-h,1,h));
                }
                break;
                
            case EXITED_SOUTH:
                exitedSouth(source);
                break;
                
            case SCROLLED_NORTH:
                JScrollBar scrollbar = bufferPane.getVerticalScrollBar();
                scrollbar.setValue(scrollbar.getValue()-scrollIncrement);
                break;
                
            case SCROLLED_SOUTH:
                scrollbar = bufferPane.getVerticalScrollBar();
                scrollbar.setValue(scrollbar.getValue()+scrollIncrement);
                break;
                
            case SCROLLED_EAST:
                scrollbar = bufferPane.getHorizontalScrollBar();
                scrollbar.setValue(scrollbar.getValue()+scrollIncrement);
                break;
                
            case SCROLLED_WEST:
                scrollbar = bufferPane.getHorizontalScrollBar();
                scrollbar.setValue(scrollbar.getValue()-scrollIncrement);
                break;
                
            case SELECTED_RECT_CHANGED:
                selectedRectChanged(source,e.selectedRect);
                break;
                
            case SPLIT_REQUESTED:
                i=buffers.indexOf(source);
                var newBufferController = newBufferController();
                newBufferController.connection = source.connection;
                newBufferController.setBackground(source.getBackground());
                add(i,newBufferController);
                bufferPanel.revalidate();
                newBufferController.focusEditor(null,null);
                newBufferController.editor.append(e.text);
                /* We don't have enough info here to restore the caret position.
                 * This should be acceptable. */
                newBufferController.editor.select(e.selectionStart,
                        e.selectionEnd);
                newBufferController.fetch(false);
                break;
                
            case RESULT_VIEW_CLOSED:
                i=buffers.indexOf(source);
                if(i<buffers.size()-1)
                {
                    buffers.get(i+1).prepend(source);
                    buffers.get(i+1).focusEditor(null,null);
                    remove(i);
                }
            case RESULT_VIEW_UPDATED:
                /* arbitrarily expose a couple of lines of table content. Just
                 * to give a visual hint that something's there. If the table is
                 * already in full view, or if scrolling would put the cursor out
                 * of view, this does nothing. */ 
                selectedRectChanged(source,new Rectangle(0,
                        (int)source.editor.getBounds().getHeight(),1,
                        source.getLineHeight() * 5));
                bufferPanel.repaint();
                break;
                
            case DRY_FETCH:
                connectionsSelector.requestFocusInWindow();
                break;

            case CHANGED:
                listener.bufferChanged();
            }
        }
    };
    
    /**Adds a buffer to the panel and wires listeners. */
    @SuppressWarnings("unchecked")
    private void add(int index,JdbcBufferController c)
    {
        c.editor.addFocusListener(new FocusListener()
        {
            @Override 
            public void focusLost(FocusEvent e)
            {
                lastFocusedBuffer = buffers.indexOf(c);
                hasSavedFocusPosition = true;
            }
            @Override 
            public void focusGained(FocusEvent e) { }
        });
        c.fetchsize = ((Number)fetchsizeField.getValue()).intValue();
        if(buffers.size()>0)
        {
            c.editor.setFont(firstBuffer().editor.getFont());
            c.sizes = (Stack<Integer>)firstBuffer().sizes.clone();
        }
        
        buffers.add(index,c);
        
        layoutPanel();
    }
    
    void remove(int index)
    {
        buffers.remove(index);
        
        layoutPanel();
    }
    
    private void layoutPanel()
    {
        bufferPanel.removeAll();
        
        for(int i=0;i<buffers.size()-1;i++)
        {
            var panelConstraints = new GridBagConstraints();
            panelConstraints.anchor = GridBagConstraints.WEST;
            panelConstraints.weightx = 1;
            panelConstraints.gridy=i;
            bufferPanel.add(buffers.get(i).panel,panelConstraints);
        }
        var panelConstraints = new GridBagConstraints();
        panelConstraints.anchor = GridBagConstraints.NORTHWEST;
        panelConstraints.weighty = panelConstraints.weightx = 1;
        panelConstraints.gridy=buffers.size();
        bufferPanel.add(buffers.get(buffers.size()-1).panel,panelConstraints);
    }
    
    private void exitedSouth(JdbcBufferController source)
    {
        int i=buffers.indexOf(source);
        if(buffers.size() <= i+1)
        {
            var newBufferController = newBufferController();
            newBufferController.connection = source.connection;
            newBufferController.setBackground(source.getBackground());
            add(i+1,newBufferController);
            bufferPanel.revalidate();
        }
        buffers.get(i+1).focusEditor(source.getCaretPositionInLine(),0);
        /* scroll relative to source, because if we added a buffer above, it's 
         * not yet laid out */
        int h = source.getLineHeight();
        selectedRectChanged(source,new Rectangle(0,
                (int)source.panel.getBounds().getHeight(),1,h));
    }

    private void selectedRectChanged(JdbcBufferController source,Rectangle rect)
    {
        Rectangle sourceRect = source.panel.getBounds();
        JViewport viewport = bufferPane.getViewport();
        Point position = viewport.getViewPosition();
        bufferPane.getViewport().scrollRectToVisible(new Rectangle(
                (int)(sourceRect.getX() + rect.getX() - position.getX()),
                (int)(sourceRect.getY() + rect.getY() - position.getY()),
                (int)rect.getWidth(),
                (int)rect.getHeight()
        ));
    }

    boolean isUnsaved()
    {
        return !buffers.stream().noneMatch((n) -> n.isUnsaved()); 
    }

    private void setSaved()
    {
        logger.log(Level.FINE,"Setting saved flag in all buffers");
        for(var buffer : buffers) buffer.setSaved();
    }

    private long lastSeenTimestamp;

    private void updateTimestamp()
    {
        lastSeenTimestamp = file.lastModified();
        logger.log(Level.FINE,"Updated lastSeenTimestamp to {0}",
                lastSeenTimestamp);
    }

    private boolean wasModified()
    {
        long lastModified = file.lastModified();
        logger.log(Level.FINE,"Timestamp on disk is {0}",lastModified);
        logger.log(Level.FINE,"Last seen timestamp is {0}",lastSeenTimestamp);
        int toleranceMs = 2000;
        return lastSeenTimestamp > 0 &&
                lastSeenTimestamp + toleranceMs < lastModified;
    }
    
    public boolean store(boolean saveAs)
    {
        if(file==null || saveAs)
        {
            var filechooser = new JFileChooser(bufferConfigSource.pwd);
            int returnVal = filechooser.showSaveDialog(bufferPanel);
            if(returnVal != JFileChooser.APPROVE_OPTION) return false;
            File file=filechooser.getSelectedFile();
            if(file.exists())
            {
                int option = JOptionPane.showConfirmDialog(
                        bufferPane,
                        "File exists. Continue?",
                        "File exists",
                        JOptionPane.YES_NO_OPTION);
                if(option != JOptionPane.YES_OPTION) return false;
            }
            this.file=file;
        }
        else if(wasModified())
        {
            int option = JOptionPane.showConfirmDialog(
                    bufferPane,
                    "File seems modified on disk. Continue?",
                    "File modified",
                    JOptionPane.YES_NO_OPTION);
            if(option != JOptionPane.YES_OPTION) return false;
        }
        if(isUnsaved()&&file.exists())
        {
            var bakfile=new File(file.getPath()+'~');
            bakfile.delete();
            file.renameTo(bakfile);
        }
        try(Writer w = new BufferedWriter(new FileWriter(file)))
        {
            store(w);
            setSaved();
            updateTimestamp();
            return true;
        }
        catch(IOException e)
        {
            JOptionPane.showMessageDialog(
                    bufferPane,
                    "Error saving: "+e.getMessage(),
                    "Error saving",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public boolean rename()
    {
        if(file == null)
        {
            JOptionPane.showMessageDialog(
                    bufferPane,
                    "Please save to a file first.",
                    "Cannot rename",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        String newname = JOptionPane.showInputDialog(
                bufferPane,
                "Please enter a new name:",
                "Rename file",
                JOptionPane.QUESTION_MESSAGE);
        File newFile = new File(file.getParentFile(),newname);
        if(!newname.isEmpty()&&!newFile.exists()&&file.renameTo(newFile))
        {
            file = newFile;
            return true;
        }
        else
        {
            JOptionPane.showMessageDialog(
                    bufferPane,
                    "Could not rename the file to \""+newname+"\"",
                    "Error renaming",
                    JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    private void store(Writer w)
    throws IOException
    {
        for(JdbcBufferController buffer : buffers)
        {
            buffer.store(w);
        }
    }
    
    void load(File f)
    throws IOException
    {
        try(var r = new LineNumberReader(new FileReader(f)))
        {
            assert buffers.size()==1 : "load only supports uninitialized panels";
            int linesRead = firstBuffer().load(r);
            file = f;
            while(linesRead>0)
            {
                var newBufferController = newBufferController();
                newBufferController.setBackground(firstBuffer().getBackground());
                linesRead = newBufferController.load(r);
                if(linesRead > 0) add(buffers.size(),newBufferController);
            }
        }
        setSaved();
        updateTimestamp();
        bufferPanel.revalidate();
        firstBuffer().focusEditor(0,0);
    }

    void commit()
    {
        onConnection((c) -> c.commit(logConsumer));
    }

    void rollback()
    {
        onConnection((c) -> c.rollback(logConsumer));
    }

    void disconnect()
    {
        onConnection((c) -> 
        {
            c.disconnect(logConsumer,() ->
            {
                listener.disconnected(c);
            });
        });
    }
    
    void restoreFocus()
    {
        buffers.get(lastFocusedBuffer).focusEditor(null,null);
    }
    
    void increaseFetchsize()
    {
        int fetchsize = ((Number)fetchsizeField.getValue()).intValue();
        if(fetchsize >= 10000) return;
        if(fetchsize >= 1000) fetchsize = 10000;
        else if(fetchsize >= 100) fetchsize = 1000;
        else if(fetchsize >= 10) fetchsize = 100;
        else fetchsize = 10;
        fetchsizeField.setValue(fetchsize);
    }

    void decreaseFetchsize()
    {
        int fetchsize = ((Number)fetchsizeField.getValue()).intValue();
        if(fetchsize <= 10) fetchsize = 1;
        else if(fetchsize <= 100) fetchsize = 10;
        else if(fetchsize <= 1000) fetchsize = 100;
        else if(fetchsize <= 10000) fetchsize = 1000;
        else fetchsize = 10000;
        fetchsizeField.setValue(fetchsize);
    }

    boolean findAndAdvance(JdbcNotebookSearchState state)
    {
        if(state.buf>=buffers.size()) return false;
        assert state.text != null;
        logger.log(Level.FINE,"Finding: {0}",state.text);
        logger.log(Level.FINE,"Buffer index: {0}",state.buf);
        logger.log(Level.FINE,"Last search location: {0}",state.loc);
        state.loc=buffers.get(state.buf).searchNext(state.loc+1,state.text);
        if(state.loc<0) 
        {
            state.buf++;
            return findAndAdvance(state);
        }
        return true;
    }
    
    private final PropertyChangeListener fetchSizeListener = (e) ->
    {
        int fetchsize = ((Number)fetchsizeField.getValue()).intValue();
        for(JdbcBufferController buffer : buffers)
        {
            buffer.fetchsize = fetchsize;
        }
    };

    private void onConnection(Consumer<ConnectionWorker> c)
    {
        ConnectionWorker selectedConnection = firstBuffer().connection;
        if(selectedConnection != null)
        {
            c.accept(selectedConnection);
        }
        else
        {
            logConsumer.accept("No connection available at "+new Date());
        };
    }
    
    private void updateConnection(ItemEvent event)
    {
        /* 
         * If the update was due to a Deselect action, don't do anything
         * with it. Note this will happen for disconnect.
         */
        if(event.getStateChange()==ItemEvent.DESELECTED) return;
        try
        {
            var item = (Connections.ConnectionState)event.getItem();
            var connection = connections.getConnection(item,logConsumer);
            connection.setAutoCommit(autocommitCb.isSelected(),logConsumer,() ->
            {
                listener.autocommitChanged(connection,autocommitCb.isSelected());
            });
            for(JdbcBufferController buffer : buffers)
            {
                buffer.connection = connection;
            }
            setBackground(item.info().background);
            restoreFocus();
        }
        catch(SQLException e)
        {
            reportDisconnect(firstBuffer().connection);
            logConsumer.accept(SQLExceptionPrinter.toString(e));
        }
    }

    void closeCurrentResultSet()
    {
        for(JdbcBufferController buffer : buffers)
        {
            buffer.closeCurrentResultSet();
        }
    }
    
    void reportDisconnect(ConnectionWorker connection)
    {
        if(firstBuffer().connection != connection) return;
        for(JdbcBufferController buffer : buffers)
        {
            buffer.connection = null;
        }
        connectionsSelector.setSelectedIndex(-1);
        connectionsSelector.repaint();
        
        setBackground(null);
    }
    
    void reportAutocommitChanged(ConnectionWorker connection,boolean enabled)
    {
        if(firstBuffer().connection != connection) return;
        autocommitCb.setSelected(enabled);
    }
}
