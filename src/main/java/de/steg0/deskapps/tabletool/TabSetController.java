package de.steg0.deskapps.tabletool;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
extends MouseAdapter
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
        tabbedPane.addMouseListener(this);
    }
    
    /**
     * Adds a tab and selects it.
     * 
     * @param unnamed whether a new, generic name should be set on the tab.
     * If <code>false</code>, the tab will not have a name; it is expected
     * that the caller sets one.
     * @return the controller responsible for the new tab.
     */
    JdbcNotebookController add(boolean unnamed)
    {
        var notebook = new JdbcNotebookController(
                parent,
                propertyHolder,
                connections
        );
        notebook.addListener(notebookListener);
        notebooks.add(notebook);
        int newIndex = tabbedPane.getComponentCount();
        if(unnamed)
        {
            String newname = "Notebook"+(unnamedNotebookCount++);
            tabbedPane.add(newname,notebook.notebookPanel);
        }
        else
        {
            tabbedPane.add(notebook.notebookPanel);
        }
        tabbedPane.setSelectedIndex(newIndex);
        return notebook;
    }
    
    void removeSelected()
    {
        JdbcNotebookController notebook=
                notebooks.get(tabbedPane.getSelectedIndex());
        if(notebook.unsaved)
        {
            int option = JOptionPane.showConfirmDialog(
                    parent,
                    "Buffer is unsaved. Close?",
                    "Unsaved buffer warning",
                    JOptionPane.YES_NO_OPTION);
            if(option!=JOptionPane.YES_OPTION)
            {
                return;
            }
        }
        notebook.closeCurrentResultSet();
        notebooks.remove(tabbedPane.getSelectedIndex());
        tabbedPane.remove(tabbedPane.getSelectedIndex());
        if(notebooks.size()==0) add(true);
    }
    
    void load()
    {
        var filechooser = new JFileChooser();
        int returnVal = filechooser.showOpenDialog(tabbedPane);
        if(returnVal != JFileChooser.APPROVE_OPTION) return;
        File file=filechooser.getSelectedFile();
        
        try(var r = new LineNumberReader(new FileReader(file)))
        {
            JdbcNotebookController notebook = add(false);
            notebook.load(r);
            notebook.file = file;
            int index = tabbedPane.getSelectedIndex();
            tabbedPane.setTitleAt(index,file.getName());
            tabbedPane.setToolTipTextAt(index,file.getPath());
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(
                    tabbedPane,
                    "Error loading: "+e.getMessage(),
                    "Error loading",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    boolean isUnsaved()
    {
        return !notebooks.stream().noneMatch((n) -> n.unsaved); 
    }
    
    @SuppressWarnings("serial")
    Action
        addAction = new AbstractAction("New")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                add(true);
            }
        },
        loadAction = new AbstractAction("Load...")
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
                if(notebook.store(false))
                {
                    tabbedPane.setTitleAt(index,notebook.file.getName());
                    tabbedPane.setToolTipTextAt(index,notebook.file.getPath());
                }
            }
        },
        saveAsAction = new AbstractAction("Save As...")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index=tabbedPane.getSelectedIndex();
                JdbcNotebookController notebook=notebooks.get(index);
                if(notebook.store(true))
                {
                    tabbedPane.setTitleAt(index,notebook.file.getName());
                    tabbedPane.setToolTipTextAt(index,notebook.file.getPath());
                }
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
        Workspace w = Workspaces.load(f);
        if(w.getFiles().length==0) add(true);
        for(String fn : w.getFiles())
        {
            File sqlFile = new File(fn);
            try(var r = new LineNumberReader(new FileReader(sqlFile)))
            {
                JdbcNotebookController notebook = add(false);
                notebook.load(r);
                notebook.file = sqlFile;
                int index = tabbedPane.getSelectedIndex();
                tabbedPane.setTitleAt(index,sqlFile.getName());
                tabbedPane.setToolTipTextAt(index,sqlFile.getPath());

            }
            catch(IOException e)
            {
                throw new IOException(fn+":\n"+e.getMessage(),e);
            }
            catch(ArrayIndexOutOfBoundsException e)
            {
                throw new ArrayIndexOutOfBoundsException(fn+":\n"+
                        e.getMessage());
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
        Workspaces.store(w,file);
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

        @Override
        public void bufferChanged()
        {
            int selectedIndex = tabbedPane.getSelectedIndex();
            tabbedPane.setTitleAt(selectedIndex,
                    "*"+tabbedPane.getTitleAt(selectedIndex));
        }
    };

    @Override public void keyTyped(KeyEvent e) { }
    @Override public void keyPressed(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e)
    {
        int index=0;
        switch(e.getKeyCode())
        {
        case KeyEvent.VK_ENTER:
            notebooks.get(tabbedPane.getSelectedIndex()).restoreFocus();
            break;
        case KeyEvent.VK_RIGHT:
            index = tabbedPane.getSelectedIndex();
            if(e.isAltDown() && index<tabbedPane.getComponentCount()-1)
            {
                String title = tabbedPane.getTitleAt(index);
                Component c = tabbedPane.getSelectedComponent();
                tabbedPane.remove(index);
                tabbedPane.add(c,index+1);
                tabbedPane.setTitleAt(index+1,title);
                JdbcNotebookController notebook = notebooks.remove(index);
                notebooks.add(index+1,notebook);
                tabbedPane.setSelectedComponent(c);
            }
            break;
        case KeyEvent.VK_LEFT:
            index = tabbedPane.getSelectedIndex();
            if(e.isAltDown() && index>0)
            {
                String title = tabbedPane.getTitleAt(index);
                Component c = tabbedPane.getSelectedComponent();
                tabbedPane.remove(index);
                tabbedPane.add(c,index-1);
                tabbedPane.setTitleAt(index-1,title);
                JdbcNotebookController notebook = notebooks.remove(index);
                notebooks.add(index-1,notebook);
                tabbedPane.setSelectedComponent(c);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if(!tabbedPane.hasFocus())
        {
             JdbcNotebookController notebook = notebooks.get(
                     tabbedPane.getSelectedIndex());
             if(notebook.hasSavedFocusPosition) notebook.restoreFocus();
        }
    }
}
