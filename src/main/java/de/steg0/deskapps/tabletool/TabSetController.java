package de.steg0.deskapps.tabletool;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

class TabSetController
implements KeyListener
{
    JFrame parent;
    PropertyHolder propertyHolder;
    
    Connections connections;
    Executor executor = Executors.newCachedThreadPool();

    JTabbedPane tabbedPane = new JTabbedPane();
    List<JdbcNotebookController> notebooks = new ArrayList<>();
    int unnamedNotebookCount;
    
    TabSetController(JFrame parent,PropertyHolder propertyHolder)
    {
        this.parent = parent;
        this.propertyHolder = propertyHolder;
        
        connections = new Connections(propertyHolder,executor);
        
        tabbedPane.addKeyListener(this);
        
        add(null);
    }
    
    void add(File f)
    {
        var notebook = new JdbcNotebookController(
                parent,
                propertyHolder,
                connections,
                actions
        );
        notebooks.add(notebook);
        if(f==null)
        {
            String newname = "Notebook"+(unnamedNotebookCount++);
            tabbedPane.add(newname,notebook.notebookPanel);
            tabbedPane.setSelectedIndex(tabbedPane.getComponentCount()-1);
        }
        else
        {
            tabbedPane.add(notebook.notebookPanel);
            tabbedPane.setSelectedIndex(tabbedPane.getComponentCount()-1);
            actions.setTabTitleFor(f);
        }
    }
    
    void removeSelected()
    {
        notebooks.remove(tabbedPane.getSelectedIndex());
        tabbedPane.remove(tabbedPane.getSelectedIndex());
        if(notebooks.size()==0) add(null);
    }
    
    void load()
    {
        var filechooser = new JFileChooser();
        int returnVal = filechooser.showOpenDialog(tabbedPane);
        if(returnVal != JFileChooser.APPROVE_OPTION) return;
        File file=filechooser.getSelectedFile();
        add(file);
        try(var r = new LineNumberReader(new FileReader(file)))
        {
            JdbcNotebookController notebook = 
                    notebooks.get(tabbedPane.getSelectedIndex());
            notebook.load(r);
            notebook.file = file;
        }
        catch(IOException e)
        {
            JOptionPane.showMessageDialog(
                    tabbedPane,
                    "Error loading: "+e.getMessage(),
                    "Error loading",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    interface Actions
    {
        void add();
        void removeSelected();
        void reportDisconnect(ConnectionWorker connection);
        void setTabTitleFor(File f);
    }

    Actions actions = new Actions()
    {
        @Override
        public void add()
        {
            TabSetController.this.add(null);
        }
        @Override
        public void removeSelected()
        {
            TabSetController.this.removeSelected();
        }
        @Override
        public void reportDisconnect(ConnectionWorker connection)
        {
            for(var notebook : notebooks)
            {
                notebook.reportDisconnect(connection);
            }
            /* 
             * Now set connection in the model to null. If we had
             * done it above, the notebooks would have reconnected
             * in their ItemListener.
             */
            connections.reportDisconnect(connection);
        }
        @Override
        public void setTabTitleFor(File f)
        {
            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(),f.getName());
        }
    };

    @Override public void keyTyped(KeyEvent e) { }
    @Override public void keyPressed(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e)
    {
        switch(e.getKeyCode())
        {
        case KeyEvent.VK_ENTER:
            notebooks.get(tabbedPane.getSelectedIndex()).restoreFocus();
            break;
        case KeyEvent.VK_T:
            if(e.isControlDown()) add(null);
            break;
        case KeyEvent.VK_W:
            if(e.isControlDown()) removeSelected();
            break;
        case KeyEvent.VK_O:
            if(e.isControlDown()) load();
        }
    }
}
