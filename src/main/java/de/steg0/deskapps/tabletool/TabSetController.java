package de.steg0.deskapps.tabletool;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
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
    }
    
    JdbcNotebookController add(File f)
    {
        var notebook = new JdbcNotebookController(
                parent,
                propertyHolder,
                connections
        );
        notebook.addListener(notebookListener);
        notebooks.add(notebook);
        int newIndex = tabbedPane.getComponentCount();
        if(f==null)
        {
            String newname = "Notebook"+(unnamedNotebookCount++);
            tabbedPane.add(newname,notebook.notebookPanel);
            tabbedPane.setSelectedIndex(newIndex);
        }
        else
        {
            tabbedPane.add(notebook.notebookPanel);
            tabbedPane.setSelectedIndex(newIndex);
            tabbedPane.setTitleAt(newIndex,f.getName());
        }
        return notebook;
    }
    
    void removeSelected()
    {
        JdbcNotebookController notebook=
                notebooks.get(tabbedPane.getSelectedIndex());
        notebook.closeCurrentResultSet();
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
        
        try(var r = new LineNumberReader(new FileReader(file)))
        {
            JdbcNotebookController notebook = add(file);
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
    
    @SuppressWarnings("serial")
    Action
        addAction = new AbstractAction("New")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                add(null);
            }
        },
        loadAction = new AbstractAction("Load")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                load();
            }
        },
        saveAction = new AbstractAction("Save")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index=tabbedPane.getSelectedIndex();
                JdbcNotebookController notebook=notebooks.get(index);
                notebook.store();
                tabbedPane.setTitleAt(index,notebook.file.getName());
            }
        },
        closeAction = new AbstractAction("Close")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                removeSelected();
            }
        };
    
    void restoreWorkspace(File f)
    throws IOException
    {
        Workspace w = new Workspaces().load(f);
        if(w.getFiles().length==0) add(null);
        for(String fn : w.getFiles())
        {
            File sqlFile = new File(fn);
            try(var r = new LineNumberReader(new FileReader(sqlFile)))
            {
                JdbcNotebookController notebook = add(sqlFile);
                notebook.load(r);
                notebook.file = sqlFile;
            }
        }
    }
    
    void saveWorkspace(File file)
    throws IOException
    {
        Workspace w = new Workspace();
        w.setFiles(notebooks
            .stream()
            .map((n) -> n.file)
            .filter(Objects::nonNull)
            .map((f) -> f.getPath())
            .toArray(String[]::new));
        new Workspaces().store(w,file);
    }
        
    JdbcNotebookController.Listener notebookListener = 
            new JdbcNotebookController.Listener()
    {
        @Override
        public void disconnected(ConnectionWorker connection)
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
        }
    }
}
