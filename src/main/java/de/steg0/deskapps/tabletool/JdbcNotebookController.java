package de.steg0.deskapps.tabletool;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
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
    
    final JFrame cellDisplay,infoDisplay;
    final PropertyHolder propertyHolder;
    final JdbcBufferConfigSource bufferConfigSource;
    
    File file;
    boolean unsaved;
    
    final ConnectionListModel connections;
    JComboBox<Connections.ConnectionState> connectionsSelector;

    JFormattedTextField fetchsizeField;
    
    JButton commitButton = new JButton("Commit");
    JButton rollbackButton = new JButton("Rollback");
    JButton disconnectButton = new JButton("Disconnect");
    JCheckBox autocommitCb = new JCheckBox("Autocommit",
            Connections.AUTOCOMMIT_DEFAULT);
    
    JScrollPane bufferPane;
    int scrollIncrement;
    
    JTextArea log = new JTextArea();
    JSplitPane logBufferPane;

    void resize()
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
    
    Consumer<String> logConsumer = (t) -> log.setText(t);
    
    List<JdbcBufferController> buffers = new ArrayList<>();
    int lastFocusedBuffer;
    boolean hasSavedFocusPosition;
    int resultviewHeight;
    JPanel bufferPanel = new JPanel(new GridBagLayout());
    JPanel notebookPanel = new JPanel(new GridBagLayout());
    
    JdbcNotebookController(
            JFrame cellDisplay,
            JFrame infoDisplay,
            PropertyHolder propertyHolder,
            Connections connections,
            Listener listener)
    {
        this.cellDisplay = cellDisplay;
        this.infoDisplay = infoDisplay;
        this.propertyHolder = propertyHolder;
        this.connections = new ConnectionListModel(connections);
        this.listener = listener;
        this.bufferConfigSource = new JdbcBufferConfigSource(propertyHolder,
                this.connections);
        
        var connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        connectionsSelector = new JComboBox<>(this.connections);
        connectionsSelector.putClientProperty("JComboBox.isTableCellEditor",
                Boolean.TRUE);
        connectionsSelector.addItemListener((e) -> updateConnection(e));
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

    void setBackground(Color bg)
    {
        if(bg==null) bg=propertyHolder.getDefaultBackground(); 
        if(bg==null) bg=buffers.get(0).defaultBackground;
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
    
    final Listener listener;
    
    class BufferPaneMouseListener extends MouseAdapter
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
    
    JdbcBufferController.Listener bufferListener = 
            new JdbcBufferController.Listener()
    {
        @Override
        public void bufferActionPerformed(JdbcBufferControllerEvent e)
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
                bufferPanel.repaint();
                break;
                
            case DRY_FETCH:
                connectionsSelector.requestFocusInWindow();
            }
        }
    };
    
    /* this listener could live in JdbcBufferController */
    class BufferDocumentListener implements DocumentListener
    {
        JdbcBufferController buffer;
        
        @Override
        public void insertUpdate(DocumentEvent e)
        {
            if(!unsaved)
            {
                unsaved=true;
                listener.bufferChanged();
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e)
        {
            insertUpdate(e);
            if(e.getLength() == 1) return;
            ExtendTextDamageEvent.send(buffer.editor,e);
        }

        @Override public void changedUpdate(DocumentEvent e) { }
    };

    /**Adds a buffer to the panel and wires listeners. */
    @SuppressWarnings("unchecked")
    private void add(int index,JdbcBufferController c)
    {
        var documentListener = new BufferDocumentListener();
        documentListener.buffer = c;
        c.addDocumentListener(documentListener);
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
        c.fetchsize = Integer.parseInt(fetchsizeField.getText());
        if(buffers.size()>0)
        {
            c.editor.setFont(buffers.get(0).editor.getFont());
            c.sizes = (Stack<Integer>)buffers.get(0).sizes.clone();
        }
        
        buffers.add(index,c);
        
        layoutPanel();
    }
    
    void remove(int index)
    {
        buffers.remove(index);
        
        layoutPanel();
    }
    
    void layoutPanel()
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
    
    void exitedSouth(JdbcBufferController source)
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

    void selectedRectChanged(JdbcBufferController source,Rectangle rect)
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

    public boolean store(boolean saveAs)
    {
        if(file==null || saveAs)
        {
            var filechooser = new JFileChooser();
            int returnVal = filechooser.showSaveDialog(bufferPanel);
            if(returnVal != JFileChooser.APPROVE_OPTION) return false;
            File file=filechooser.getSelectedFile();
            if(file.exists())
            {
                int option = JOptionPane.showConfirmDialog(
                        bufferPanel,
                        "File exists. Continue?",
                        "File exists",
                        JOptionPane.YES_NO_OPTION);
                if(option != JOptionPane.YES_OPTION) return false;
            }
            this.file=file;
        }
        if(unsaved&&file.exists())
        {
            var bakfile=new File(file.getPath()+'~');
            bakfile.delete();
            file.renameTo(bakfile);
        }
        try(Writer w = new BufferedWriter(new FileWriter(file)))
        {
            store(w);
            unsaved=false;
            return true;
        }
        catch(IOException e)
        {
            JOptionPane.showMessageDialog(
                    bufferPanel,
                    "Error saving: "+e.getMessage(),
                    "Error saving",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**blocking */
    void store(Writer w)
    throws IOException
    {
        for(JdbcBufferController buffer : buffers)
        {
            buffer.store(w);
        }
    }
    
    /**blocking */
    void load(LineNumberReader r)
    throws IOException
    {
        assert buffers.size()==1 : "load only supports uninitialized panels";
        int linesRead = buffers.get(0).load(r);
        while(linesRead>0)
        {
            var newBufferController = newBufferController();
            newBufferController.setBackground(buffers.get(0).getBackground());
            linesRead = newBufferController.load(r);
            if(linesRead > 0) add(buffers.size(),newBufferController);
        }
        unsaved=false;
        bufferPanel.revalidate();
        buffers.get(0).focusEditor(0,0);
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

    int lastSearchBuf;
    int lastSearchLoc=-1;
    String lastSearchText;

    void find()
    {
        if(lastSearchBuf>=buffers.size()) return;
        if(lastSearchText==null) return;
        logger.log(Level.FINE,"Finding: {0}",lastSearchText);
        logger.log(Level.FINE,"Buffer index: {0}",lastSearchBuf);
        logger.log(Level.FINE,"Last search location: {0}",lastSearchLoc);
        lastSearchLoc=buffers.get(lastSearchBuf).searchNext(lastSearchLoc+1,
                lastSearchText);
        if(lastSearchLoc<0) 
        {
            lastSearchBuf++;
            find();
        }
    }
    
    PropertyChangeListener fetchSizeListener = (e) ->
    {
        int fetchsize = ((Number)fetchsizeField.getValue()).intValue();
        for(JdbcBufferController buffer : buffers)
        {
            buffer.fetchsize = fetchsize;
        }
    };

    void onConnection(Consumer<ConnectionWorker> c)
    {
        ConnectionWorker selectedConnection = buffers.get(0).connection;
        if(selectedConnection != null)
        {
            c.accept(selectedConnection);
        }
        else
        {
            logConsumer.accept("No connection available at "+new Date());
        };
    }
    
    /**blocking */
    void updateConnection(ItemEvent event)
    {
        /* 
         * If the update was due to a Deselect action, don't do anything
         * with it. Note this will happen for disconnect.
         */
        if(event.getStateChange()==ItemEvent.DESELECTED) return;
        try
        {
            var item = (Connections.ConnectionState)event.getItem();
            var connection = connections.getConnection(item);
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
            connectionsSelector.setSelectedIndex(-1);
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
        if(buffers.get(0).connection != connection) return;
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
        if(buffers.get(0).connection != connection) return;
        autocommitCb.setSelected(enabled);
    }
}
