package de.steg0.deskapps.tabletool;

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
import java.util.function.Consumer;

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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.NumberFormatter;

/**
 * Represents a "notebook", i. e. a series of text field/result table pairs that
 * operate on a selectable {@link ConnectionWorker}.
 */
class JdbcNotebookController
{
    static final int DEFAULT_FETCH_SIZE = 300;
    
    interface Listener extends EventListener
    {
        void disconnected(ConnectionWorker connection);
        void bufferChanged();
    }
    
    final JFrame parent;
    final PropertyHolder propertyHolder;
    
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
    
    JTextArea log = new JTextArea();
    Consumer<String> logConsumer = (t) -> log.setText(t);
    
    {
        log.setEditable(false);
    }
    
    List<JdbcBufferController> buffers = new ArrayList<>();
    int lastFocusedBuffer;
    boolean hasSavedFocusPosition;
    JPanel bufferPanel = new JPanel(new GridBagLayout());
    JPanel notebookPanel = new JPanel(new GridBagLayout());
    
    JdbcNotebookController(
            JFrame parent,
            PropertyHolder propertyHolder,
            Connections connections)
    {
        this.parent = parent;
        this.propertyHolder = propertyHolder;
        this.connections = new ConnectionListModel(connections);
        
        var connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        connectionsSelector = new JComboBox<>(this.connections);
        connectionsSelector.putClientProperty("JComboBox.isTableCellEditor",
                Boolean.TRUE);
        connectionsSelector.addItemListener((e) -> updateConnection(e));
        connectionPanel.add(connectionsSelector);
        
        connectionPanel.add(new JLabel("Fetch:"));
        
        NumberFormat format = NumberFormat.getIntegerInstance();
        NumberFormatter numberFormatter = new NumberFormatter(format);
        numberFormatter.setAllowsInvalid(false); 
        fetchsizeField = new JFormattedTextField(numberFormatter);
        fetchsizeField.setColumns(4);
        fetchsizeField.setValue(DEFAULT_FETCH_SIZE);
        fetchsizeField.addPropertyChangeListener("value",fetchSizeListener);
        connectionPanel.add(fetchsizeField);
        
        commitButton.addActionListener((e) -> 
        {
            onConnection((c) -> c.commit(logConsumer));
        });
        connectionPanel.add(commitButton);
        
        rollbackButton.addActionListener((e) -> 
        {
            onConnection((c) -> c.rollback(logConsumer));
        });
        connectionPanel.add(rollbackButton);
        
        disconnectButton.addActionListener((e) ->
        {
            onConnection((c) -> 
            {
                c.disconnect(logConsumer,() ->
                {
                    for(Listener l : listeners.getListeners(Listener.class))
                    {
                        l.disconnected(c);
                    }
                });
            });
        });
        connectionPanel.add(disconnectButton);
        
        autocommitCb.addActionListener((e) -> 
        {
            onConnection((c) -> c.setAutoCommit(
                    autocommitCb.isSelected(),logConsumer));
        });
        connectionPanel.add(autocommitCb);
        
        var connectionPanelConstraints = new GridBagConstraints();
        connectionPanelConstraints.anchor = GridBagConstraints.EAST;
        connectionPanelConstraints.weightx = 1;
        connectionPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        
        notebookPanel.add(connectionPanel,connectionPanelConstraints);
        
        var buffer = new JdbcBufferController(parent,logConsumer);
        add(0,buffer);

        var logBufferPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        logBufferPane.setResizeWeight(.85);
        
        bufferPanel.setBackground(buffer.editor.getBackground());
        bufferPane = new JScrollPane(bufferPanel);
        bufferPane.getVerticalScrollBar().setUnitIncrement(16);
        bufferPane.getHorizontalScrollBar().setUnitIncrement(16);
        bufferPane.addMouseListener(new BufferPaneMouseListener());
        logBufferPane.add(bufferPane);
        var logPane = new JScrollPane(log);
        logBufferPane.add(logPane);
        
        var bufferPaneConstraints = new GridBagConstraints();
        bufferPaneConstraints.fill = GridBagConstraints.BOTH;
        bufferPaneConstraints.weighty = bufferPaneConstraints.weightx = 1;
        bufferPaneConstraints.gridy = 1;
        notebookPanel.add(logBufferPane,bufferPaneConstraints);
    }
    
    EventListenerList listeners = new EventListenerList();
    
    void addListener(Listener l)
    {
        listeners.add(Listener.class,l);
    }

    class BufferPaneMouseListener extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            int viewportY = (int)bufferPane.getViewport()
                    .getViewPosition().getY();
            Point reference = new Point(0,e.getY() + viewportY);
            var component = bufferPanel.getComponentAt(reference);
            for(var buffer : buffers)
            {
                if(buffer.panel == component)
                {
                    int bufferY = (int)buffer.panel.getLocation().getY();
                    buffer.focusEditor(null,e.getY() + viewportY - bufferY);
                    return;
                }
            }
            JdbcBufferController buffer = buffers.get(buffers.size() - 1);
            int bufferY = (int)buffer.panel.getLocation().getY();
            buffer.focusEditor(null,e.getY() + viewportY - bufferY);
        }
    }
    
    JdbcBufferController.Listener bufferListener = 
            new JdbcBufferController.Listener()
    {
        @Override
        public void exitedSouth(JdbcBufferController source)
        {
            int i=buffers.indexOf(source);
            if(buffers.size() <= i+1)
            {
                var newBufferController = new JdbcBufferController(
                        parent,logConsumer);
                newBufferController.connection = source.connection;
                add(i+1,newBufferController);
                bufferPanel.revalidate();
            }
            buffers.get(i+1).focusEditor(source.getCaretPositionInLine(),0);
            /* scroll relative to source, because if we added a buffer above,
             * it's not yet laid out */
            selectedRectChanged(source,new Rectangle(0,
                    (int)source.panel.getBounds().getHeight(),1,16));
        }

        @Override
        public void exitedNorth(JdbcBufferController source)
        {
            int i=buffers.indexOf(source);
            if(i > 0) 
            {
                JdbcBufferController target = buffers.get(i-1);
                target.focusEditor(source.getCaretPositionInLine(),-1);
                selectedRectChanged(target,new Rectangle(0,
                        (int)target.editor.getBounds().getHeight()-16,1,16));
            }
        }
        
        @Override
        public void scrolledSouth(JdbcBufferController source)
        {
            JScrollBar scrollbar = bufferPane.getVerticalScrollBar();
            scrollbar.setValue(scrollbar.getValue()+16);
        }

        @Override
        public void scrolledNorth(JdbcBufferController source)
        {
            JScrollBar scrollbar = bufferPane.getVerticalScrollBar();
            scrollbar.setValue(scrollbar.getValue()-16);
        }

        @Override
        public void selectedRectChanged(JdbcBufferController source,
                Rectangle rect)
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

        @Override
        public void splitRequested(JdbcBufferController source,String text,
                int selectionStart,int selectionEnd)
        {
            int i=buffers.indexOf(source);
            var newBufferController = new JdbcBufferController(
                    parent,logConsumer);
            newBufferController.connection = source.connection;
            add(i,newBufferController);
            bufferPanel.revalidate();
            newBufferController.focusEditor(null,null);
            newBufferController.appendText(text);
            /* We don't have enough info here to restore the caret position.
             * This should be acceptable. */
            newBufferController.select(selectionStart,selectionEnd);
            newBufferController.fetch(false);
        }

        @Override
        public void resultViewClosed(JdbcBufferController source)
        {
            int i=buffers.indexOf(source);
            if(i<buffers.size()-1)
            {
                buffers.get(i+1).prepend(source);
                remove(i);
            }
            bufferPanel.repaint();
        }

        @Override
        public void resultViewUpdated(JdbcBufferController source)
        {
            bufferPanel.repaint();
        }

        @Override
        public void promptConnection()
        {
            connectionsSelector.requestFocusInWindow();
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
                for(Listener l : listeners.getListeners(Listener.class))
                {
                    l.bufferChanged();
                }
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e)
        {
            insertUpdate(e);
            JTextArea editor = buffer.editor;
            if(editor.getSelectionStart()==editor.getSelectionEnd()) return;
            ExtendTextDamageEvent.send(editor,e);
        }

        @Override public void changedUpdate(DocumentEvent e) { }
    };

    /**Adds a buffer to the panel and wires listeners. */
    void add(int index,JdbcBufferController c)
    {
        c.addListener(bufferListener);
        var documentListener = new BufferDocumentListener();
        documentListener.buffer = c;
        c.addDocumentListener(documentListener);
        c.addEditorFocusListener(new FocusListener()
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
            var newBufferController = new JdbcBufferController(
                    parent,logConsumer);
            linesRead = newBufferController.load(r);
            if(linesRead > 0) add(buffers.size(),newBufferController);
        }
        unsaved=false;
        bufferPanel.revalidate();
        buffers.get(0).focusEditor(0,0);
    }
    
    void restoreFocus()
    {
        buffers.get(lastFocusedBuffer).focusEditor(null,null);
    }
    
    PropertyChangeListener fetchSizeListener = (e) ->
    {
        int  fetchsize = Integer.parseInt(fetchsizeField.getText());
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
            var connection = connections.getConnection(event.getItem());
            connection.setAutoCommit(autocommitCb.isSelected(),logConsumer);
            for(JdbcBufferController buffer : buffers)
            {
                buffer.connection = connection;
            }
            restoreFocus();
        }
        catch(SQLException e)
        {
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
    }
    
}
