package de.steg0.deskapps.tabletool;

import static java.awt.event.ActionEvent.CTRL_MASK;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
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
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

class TabSetController
extends MouseAdapter
implements KeyListener
{
    int MAX_RECENTS_SIZE=500;
    
    JFrame parent;
    PropertyHolder propertyHolder;
    
    Connections connections;
    Executor executor = Executors.newCachedThreadPool();

    TabSetController(JFrame parent,PropertyHolder propertyHolder)
    {
        this.parent = parent;
        this.propertyHolder = propertyHolder;
        
        connections = new Connections(propertyHolder,executor);
    }

    JTabbedPane tabbedPane = new JTabbedPane();
    
    class SelectTabAction extends AbstractAction
    {
        int tabindex;
        SelectTabAction(int tabindex)
        {
            this.tabindex = tabindex;
        }
        @Override public void actionPerformed(ActionEvent e)
        {
            tabbedPane.setSelectedIndex(tabindex);
        }
    }
    
    {
        tabbedPane.addKeyListener(this);
        tabbedPane.addMouseListener(this);
        
        var im = tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(getKeyStroke(KeyEvent.VK_1,CTRL_MASK),"Select Tab 1");
        im.put(getKeyStroke(KeyEvent.VK_2,CTRL_MASK),"Select Tab 2");
        im.put(getKeyStroke(KeyEvent.VK_3,CTRL_MASK),"Select Tab 3");
        im.put(getKeyStroke(KeyEvent.VK_4,CTRL_MASK),"Select Tab 4");
        im.put(getKeyStroke(KeyEvent.VK_5,CTRL_MASK),"Select Tab 5");
        im.put(getKeyStroke(KeyEvent.VK_6,CTRL_MASK),"Select Tab 6");
        im.put(getKeyStroke(KeyEvent.VK_7,CTRL_MASK),"Select Tab 7");
        im.put(getKeyStroke(KeyEvent.VK_8,CTRL_MASK),"Select Tab 8");
        im.put(getKeyStroke(KeyEvent.VK_9,CTRL_MASK),"Select Tab 9");
        im.put(getKeyStroke(KeyEvent.VK_0,CTRL_MASK),"Select Tab 10");
        var am = tabbedPane.getActionMap();
        am.put("Select Tab 1",new SelectTabAction(0));
        am.put("Select Tab 2",new SelectTabAction(1));
        am.put("Select Tab 3",new SelectTabAction(2));
        am.put("Select Tab 4",new SelectTabAction(3));
        am.put("Select Tab 5",new SelectTabAction(4));
        am.put("Select Tab 6",new SelectTabAction(5));
        am.put("Select Tab 7",new SelectTabAction(6));
        am.put("Select Tab 8",new SelectTabAction(7));
        am.put("Select Tab 9",new SelectTabAction(8));
        am.put("Select Tab 10",new SelectTabAction(9));
        
        /* https://stackoverflow.com/questions/811248/how-can-i-use-drag-and-drop-in-swing-to-get-file-path */
        tabbedPane.setDropTarget(new DropTarget()
        {
            public synchronized void drop(DropTargetDropEvent event)
            {
                try
                {
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable t = event.getTransferable();
                    for(var flavor : t.getTransferDataFlavors())
                    {
                        if(flavor.isFlavorJavaFileListType())
                        {
                            var files = (List<?>)t.getTransferData(
                                    DataFlavor.javaFileListFlavor);
                            for(var file : files) load((File)file);
                            event.dropComplete(true);
                            return;
                        }
                    }
                    event.dropComplete(false);
                }
                catch(Exception e)
                {
                    event.dropComplete(false);
                }
            }
        });
    }
    
    List<JdbcNotebookController> notebooks = new ArrayList<>();
    int unnamedNotebookCount;
    
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
    
    Deque<String> recents = new LinkedList<>();
    
    JMenu getRecentsMenu()
    {
        JMenu menu = new JMenu("Recent");
        
        List<String> paths = new ArrayList<>(new LinkedHashSet<>(recents));
        Collections.reverse(paths);
        int count=0;
        for(var path : paths)
        {
            if(count++>20) break;
            var menuItem = new JMenuItem(path);
            menuItem.addActionListener((e) -> load(new File(path)));
            menu.add(menuItem);
        }
        
        return menu;
    }
    
    void addRecent(File file)
    {
        recents.add(file.getPath());
        while(recents.size() > MAX_RECENTS_SIZE) recents.removeFirst();
        recreateMenuBar();
    }
    
    void load(File file)
    {
        if(file==null)
        {
            var filechooser = new JFileChooser();
            int returnVal = filechooser.showOpenDialog(tabbedPane);
            if(returnVal != JFileChooser.APPROVE_OPTION) return;
            file=filechooser.getSelectedFile();
        }
        
        try(var r = new LineNumberReader(new FileReader(file)))
        {
            JdbcNotebookController notebook = add(false);
            notebook.load(r);
            notebook.file = file;
            int index = tabbedPane.getSelectedIndex();
            tabbedPane.setTitleAt(index,file.getName());
            tabbedPane.setToolTipTextAt(index,file.getPath());
            addRecent(file);
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
                load(null);
            }
        },
        saveAction = new AbstractAction("Save")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index=tabbedPane.getSelectedIndex();
                JdbcNotebookController notebook=notebooks.get(index);
                boolean newBuffer=notebook.file==null;
                if(notebook.store(false))
                {
                    tabbedPane.setTitleAt(index,notebook.file.getName());
                    tabbedPane.setToolTipTextAt(index,notebook.file.getPath());
                    if(newBuffer) addRecent(notebook.file);
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
                    addRecent(notebook.file);
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
    
    void recreateMenuBar()
    {
        var menubar = new JMenuBar();
        
        JMenu menu;
        
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        JMenuItem item;

        item = new JMenuItem(addAction);
        item.setMnemonic(KeyEvent.VK_N);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_T,CTRL_MASK));
        menu.add(item);
        
        item = new JMenuItem(loadAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_O,CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_O);
        menu.add(item);

        item = getRecentsMenu();
        menu.add(item);
        
        item = new JMenuItem(saveAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_S,CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_S);
        menu.add(item);

        item = new JMenuItem(saveAsAction);
        item.setMnemonic(KeyEvent.VK_A);
        menu.add(item);

        item = new JMenuItem(closeAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_W,CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_C);
        menu.add(item);

        menubar.add(menu);
        
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        
        item = new JMenuItem(new HelpAction(parent));
        item.setMnemonic(KeyEvent.VK_R);
        menu.add(item);
        
        menubar.add(menu);
        
        parent.setJMenuBar(menubar);
    }
    
    void restoreWorkspace(File f)
    throws IOException
    {
        Workspace w = Workspaces.load(f);
        
        if(w.getRecentFiles() != null)
        {
            for(var path : w.getRecentFiles()) recents.add(path);
        }
        
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
        w.setRecentFiles(recents.toArray(new String[recents.size()]));
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

        @Override
        public void autocommitChanged(ConnectionWorker connection,
                boolean enabled)
        {
            for(var notebook : notebooks)
            {
                notebook.reportAutocommitChanged(connection,enabled);
            }
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
    
    int lastClicked=-1;

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if(tabbedPane.getSelectedIndex() == lastClicked) return;
        lastClicked = tabbedPane.getSelectedIndex();
        if(!tabbedPane.hasFocus())
        {
            JdbcNotebookController notebook = notebooks.get(lastClicked);
            if(notebook.hasSavedFocusPosition) notebook.restoreFocus();
        }
    }
}
