package de.steg0.deskapps.tabletool;

import static java.awt.event.ActionEvent.ALT_MASK;
import static java.awt.event.ActionEvent.CTRL_MASK;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.Component;
import java.awt.Desktop;
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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.swing.SwingUtilities;

class TabSetController
extends MouseAdapter
implements KeyListener
{
    static final int MAX_RECENTS_SIZE=500;
    
    private Logger logger = Logger.getLogger("tabtype");

    private final JFrame parent,cellDisplay,infoDisplay;
    private final PropertyHolder propertyHolder;
    
    private Connections connections;
    private final Executor executor = Executors.newCachedThreadPool();

    final JTabbedPane tabbedPane = new JTabbedPane();

    private File workspaceFile;
    
    TabSetController(JFrame parent,JFrame cellDisplay,JFrame infoDisplay,
            PropertyHolder propertyHolder,File workspaceFile)
    {
        this.parent = parent;
        this.cellDisplay = cellDisplay;
        this.infoDisplay = infoDisplay;
        this.propertyHolder = propertyHolder;
        this.workspaceFile = workspaceFile;

        tabbedPane.setTabPlacement(propertyHolder.getTabPlacement());
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        
        connections = new Connections(propertyHolder,executor);
    }

    private long lastSelectTabActionTime;

    class SelectTabAction extends AbstractAction
    {
        int tabindex,multiDigitTabindex;
        SelectTabAction(int tabindex,int multiDigitTabindex)
        {
            this.tabindex = tabindex;
            this.multiDigitTabindex = multiDigitTabindex;
        }
        private int computeMultiDigitIndex(long time)
        {
            int index=tabindex;
            if(time-lastSelectTabActionTime < 700)
            {
                int firstDigit = tabbedPane.getSelectedIndex() + 1;
                index = firstDigit * 10 + multiDigitTabindex;
                if(index >= tabbedPane.getTabCount()) index = tabindex;
            }
            if(index >= tabbedPane.getTabCount())
            {
                index = tabbedPane.getSelectedIndex();
            }
            lastSelectTabActionTime = time;
            return index;
        }
        @Override public void actionPerformed(ActionEvent e)
        {
            tabbedPane.setSelectedIndex(computeMultiDigitIndex(e.getWhen()));
            NotebookController c = notebooks.get(
                    tabbedPane.getSelectedIndex());
            if(c.hasSavedFocusPosition) c.restoreFocus();
        }
    }
    
    class ZoomAction extends AbstractAction
    {
        double factor;
        ZoomAction(double factor)
        {
            this.factor = factor;
        }
        @Override public void actionPerformed(ActionEvent e)
        {
            int selected = tabbedPane.getSelectedIndex();
            NotebookController notebook = notebooks.get(selected);
            notebook.zoom(factor);
        }
    }

    private NotebookSearchState searchState=new NotebookSearchState();

    Action
        addAction = new AbstractAction("New")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                add(-1);
            }
        },
        loadAction = new AbstractAction("Load...")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                load(null);
            }
        },
        openContainingFolderAction = new AbstractAction(
                "Open Containing Folder")
        {
            @Override public void actionPerformed(ActionEvent event)
            {
                int index=tabbedPane.getSelectedIndex();
                NotebookController notebook=notebooks.get(index);
                if(notebook.file==null||notebook.file.getParentFile()==null)
                {
                    JOptionPane.showMessageDialog(
                        tabbedPane,
                        "No folder available for current file",
                        "Error opening containing folder",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                File parent = notebook.file.getParentFile();
                try
                {
                    Desktop.getDesktop().open(parent);
                }
                catch(IOException e)
                {
                    JOptionPane.showMessageDialog(
                            tabbedPane,
                            "Error opening "+parent+": "+e.getMessage(),
                            "Error opening containing folder",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        },
        saveAction = new AbstractAction("Save")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index=tabbedPane.getSelectedIndex();
                NotebookController notebook=notebooks.get(index);
                boolean newBuffer=notebook.file==null;
                if(notebook.store(false))
                {
                    retitle();
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
                NotebookController notebook=notebooks.get(index);
                if(notebook.store(true))
                {
                    retitle();
                    tabbedPane.setToolTipTextAt(index,notebook.file.getPath());
                    addRecent(notebook.file);
                }
            }
        },
        setWorkspaceAction = new AbstractAction("Set Workspace File...")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                var filechooser = new JFileChooser(getPwd());
                int returnVal = filechooser.showSaveDialog(parent);
                if(returnVal != JFileChooser.APPROVE_OPTION) return;
                var newFile=filechooser.getSelectedFile();
                if(newFile.exists())
                {
                    int option = JOptionPane.showConfirmDialog(
                            parent,
                            "File exists and will be overwritten on exit. " +
                            "Continue?",
                            "File exists",
                            JOptionPane.YES_NO_OPTION);
                    if(option != JOptionPane.YES_OPTION) return;
                }
                workspaceFile = newFile;
            }
        },
        renameAction = new AbstractAction("Rename...")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index=tabbedPane.getSelectedIndex();
                NotebookController notebook=notebooks.get(index);
                if(notebook.rename())
                {
                    retitle();
                    tabbedPane.setToolTipTextAt(index,notebook.file.getPath());
                    addRecent(notebook.file);
                }
            }
        },
        cloneAction = new AbstractAction("Clone")
        {
            {
                putValue(Action.SHORT_DESCRIPTION,"""
                        Loads contents of the selected tab, excluding \
                        live result set data, into a new notebook.""");
            }
            @Override public void actionPerformed(ActionEvent e)
            {
                cloneTab();
            }
        },
        revertAction = new AbstractAction("Revert")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                revert();
            }
        },
        closeAction = new AbstractAction("Close")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                removeSelected();
            }
        },
        openPropertiesAction = new AbstractAction("Edit Properties")
        {
            @Override public void actionPerformed(ActionEvent event)
            {
                try
                {
                    for(File f : propertyHolder.propertiesfiles)
                    {
                        if(!f.exists()) Files.createFile(f.toPath());
                        Desktop.getDesktop().open(f);
                    }
                }
                catch(IOException e)
                {
                    JOptionPane.showMessageDialog(
                            tabbedPane,
                            "Error opening properties files:\n"+e.getMessage(),
                            "Error opening properties",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        },
        refreshPropertiesAction = new AbstractAction("Refresh Properties")
        {
            @Override public void actionPerformed(ActionEvent event)
            {
                try
                {
                    propertyHolder.load();
                    int oldSize = connections.getSize();
                    connections.refresh(propertyHolder);
                    for(var notebook : notebooks)
                    {
                        notebook.connections.notifyIntervalAdded(oldSize);
                    }
                }
                catch(IOException e)
                {
                    JOptionPane.showMessageDialog(
                            tabbedPane,
                            "Error refreshing files:\n"+e.getMessage(),
                            "Error refreshing properties",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        },
        selectPreviousTabAction = new AbstractAction("Select Previous Tab")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int selected = tabbedPane.getSelectedIndex();
                if(selected==0) selected=tabbedPane.getTabCount();
                tabbedPane.setSelectedIndex(selected-1);
                NotebookController c = notebooks.get(
                        tabbedPane.getSelectedIndex());
                if(c.hasSavedFocusPosition) c.restoreFocus();
            }
        },
        selectNextTabAction = new AbstractAction("Select Next Tab")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int selected = tabbedPane.getSelectedIndex();
                if(selected==tabbedPane.getTabCount()-1) selected=-1;
                tabbedPane.setSelectedIndex(selected+1);
                NotebookController c = notebooks.get(
                        tabbedPane.getSelectedIndex());
                if(c.hasSavedFocusPosition) c.restoreFocus();
            }
        },
        moveTabLeftAction = new AbstractAction("Move Tab Left")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index = tabbedPane.getSelectedIndex();
                if(index>0)
                {
                    Component c = tabbedPane.getSelectedComponent();
                    tabbedPane.remove(index);
                    tabbedPane.add(c,index-1);
                    NotebookController notebook = notebooks.remove(index);
                    notebooks.add(index-1,notebook);
                    retitle();
                    tabbedPane.setSelectedComponent(c);
                }
        
            }
        },
        moveTabRightAction = new AbstractAction("Move Tab Right")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index = tabbedPane.getSelectedIndex();
                if(index<tabbedPane.getTabCount()-1)
                {
                    Component c = tabbedPane.getSelectedComponent();
                    tabbedPane.remove(index);
                    tabbedPane.add(c,index+1);
                    NotebookController notebook = notebooks.remove(index);
                    notebooks.add(index+1,notebook);
                    retitle();
                    tabbedPane.setSelectedComponent(c);
                }
            }
        },
        increaseFetchsizeAction = new AbstractAction("Fetchsize+")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index=tabbedPane.getSelectedIndex();
                notebooks.get(index).increaseFetchsize();
            }
        },
        decreaseFetchsizeAction = new AbstractAction("Fetchsize-")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index=tabbedPane.getSelectedIndex();
                notebooks.get(index).decreaseFetchsize();
            }
        },
        commitAction = new AbstractAction("Commit")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index=tabbedPane.getSelectedIndex();
                notebooks.get(index).commit();
            }
        },
        rollbackAction = new AbstractAction("Rollback")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index=tabbedPane.getSelectedIndex();
                notebooks.get(index).rollback();
            }
        },
        openAction = new AbstractAction("Open")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index=tabbedPane.getSelectedIndex();
                notebooks.get(index).openConnection();
            }
        },
        disconnectAction = new AbstractAction("Disconnect")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                int index=tabbedPane.getSelectedIndex();
                notebooks.get(index).disconnect();
            }
        },
        findAction = new AbstractAction("Find")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                String text = JOptionPane.showInputDialog(parent,"Find text:");
                if(text==null) return;
                searchState.reset(tabbedPane.getSelectedIndex());
                searchState.text=text;
                find();
            }
        },
        findNextAction = new AbstractAction("Find Next")
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                if(searchState.text==null) return;
                tabbedPane.setSelectedIndex(searchState.tab);
                find();
            }
        };

    private void find()
    {
        NotebookController notebook = notebooks.get(searchState.tab);
        boolean hasMatch = false;
        while(!(hasMatch=notebook.findAndAdvance(searchState)) && 
                searchState.tab < notebooks.size() - 1)
        {
            searchState.reset(searchState.tab+1);
            tabbedPane.setSelectedIndex(searchState.tab);
            notebook = notebooks.get(searchState.tab);
        }
        if(!hasMatch) JOptionPane.showMessageDialog(parent,
                "No match found in remaining text",
                "No match found",
                JOptionPane.INFORMATION_MESSAGE);
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
        im.put(getKeyStroke(KeyEvent.VK_LEFT,ALT_MASK),"Select Previous Tab");
        im.put(getKeyStroke(KeyEvent.VK_RIGHT,ALT_MASK),"Select Next Tab");
        im.put(getKeyStroke(KeyEvent.VK_LEFT,ALT_MASK|CTRL_MASK),
                "Move Tab Left");
        im.put(getKeyStroke(KeyEvent.VK_RIGHT,ALT_MASK|CTRL_MASK),
                "Move Tab Right");
        im.put(getKeyStroke(KeyEvent.VK_EQUALS,CTRL_MASK),"Zoom+");
        im.put(getKeyStroke(KeyEvent.VK_MINUS,CTRL_MASK),"Zoom-");
        im.put(getKeyStroke(KeyEvent.VK_UP,ALT_MASK),"Fetchsize+");
        im.put(getKeyStroke(KeyEvent.VK_DOWN,ALT_MASK),"Fetchsize-");
        im.put(getKeyStroke(KeyEvent.VK_F,CTRL_MASK),"Find");
        im.put(getKeyStroke(KeyEvent.VK_F3,0),"Find Next");
        var am = tabbedPane.getActionMap();
        am.put("Select Tab 1",new SelectTabAction(0,0));
        am.put("Select Tab 2",new SelectTabAction(1,1));
        am.put("Select Tab 3",new SelectTabAction(2,2));
        am.put("Select Tab 4",new SelectTabAction(3,3));
        am.put("Select Tab 5",new SelectTabAction(4,4));
        am.put("Select Tab 6",new SelectTabAction(5,5));
        am.put("Select Tab 7",new SelectTabAction(6,6));
        am.put("Select Tab 8",new SelectTabAction(7,7));
        am.put("Select Tab 9",new SelectTabAction(8,8));
        am.put("Select Tab 10",new SelectTabAction(9,-1));
        am.put("Select Previous Tab",selectPreviousTabAction);
        am.put("Select Next Tab",selectNextTabAction);
        am.put("Move Tab Left",moveTabLeftAction);
        am.put("Move Tab Right",moveTabRightAction);
        am.put("Zoom+",new ZoomAction(1.3));
        am.put("Zoom-",new ZoomAction(1.0/1.3));
        am.put("Fetchsize+",increaseFetchsizeAction);
        am.put("Fetchsize-",decreaseFetchsizeAction);
        am.put("Find",findAction);
        am.put("Find Next",findNextAction);
        
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
    
    private final List<NotebookController> notebooks = new ArrayList<>();

    /**
     * Adds a tab and selects it. Use an index &lt; 0 to add to the end.
     * 
     * @return the controller responsible for the new tab.
     */
    NotebookController add(int index)
    {
        var notebook = new NotebookController(
                parent,
                cellDisplay,
                infoDisplay,
                propertyHolder,
                connections,
                getPwd(),
                notebookListener
        );
        int newIndex = index<0?tabbedPane.getTabCount():index;
        notebooks.add(newIndex,notebook);
        tabbedPane.add(notebook.notebookPanel,newIndex);
        retitle();
        tabbedPane.setSelectedIndex(newIndex);
        return notebook;
    }
    
    void removeSelected()
    {
        NotebookController notebook=
                notebooks.get(tabbedPane.getSelectedIndex());
        if(notebook.isUnsaved())
        {
            int option = JOptionPane.showConfirmDialog(
                    parent,
                    "Notebook is unsaved. Close?",
                    "Unsaved notebook warning",
                    JOptionPane.YES_NO_OPTION);
            if(option!=JOptionPane.YES_OPTION)
            {
                return;
            }
        }
        notebook.closeCurrentResultSet();
        notebooks.remove(tabbedPane.getSelectedIndex());
        tabbedPane.remove(tabbedPane.getSelectedIndex());
        retitle();
        if(notebooks.size()==0) add(-1);
        SwingUtilities.invokeLater(() -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            NotebookController c = notebooks.get(selectedIndex);
            if(c.hasSavedFocusPosition) c.restoreFocus();
        });
    }
    
    private final Deque<String> recents = new LinkedList<>();
    
    JMenu getRecentsMenu()
    {
        JMenu menu = new JMenu("Recent");
        
        List<String> paths = new ArrayList<>(recents);
        Collections.reverse(paths);
        int count=0;
        for(var path : new LinkedHashSet<>(paths))
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

    private File getPwd()
    {
        if(workspaceFile!=null)
        {
            File absoluteFile = workspaceFile.getAbsoluteFile();
            logger.log(Level.FINE,"Workspace absolute file is {0}",
                    absoluteFile);
            File parentFile = absoluteFile.getParentFile();
            logger.log(Level.FINE,"Using parent file {0}",parentFile);
            return parentFile;
        }
        return null;
    }

    void load(File file)
    {
        if(file==null)
        {
            var filechooser = new JFileChooser(getPwd());
            int returnVal = filechooser.showOpenDialog(tabbedPane);
            if(returnVal != JFileChooser.APPROVE_OPTION) return;
            file=filechooser.getSelectedFile();
        }
        
        int oldSelectedIndex = tabbedPane.getSelectedIndex();
        try
        {
            NotebookController notebook = add(-1);
            notebook.load(file);
            int index = tabbedPane.getSelectedIndex();
            retitle();
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
            removeSelected();
            tabbedPane.setSelectedIndex(oldSelectedIndex);
        }
    }

    void revert()
    {
        int index=tabbedPane.getSelectedIndex();
        NotebookController notebook=notebooks.get(index);
        if(notebook.file == null)
        {
            JOptionPane.showMessageDialog(
                    tabbedPane,
                    "Error reverting: Notebook not present on disk",
                    "Error reverting",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        int option = JOptionPane.showConfirmDialog(
                parent,
                "Revert to saved?",
                "Confirm revert notebook",
                JOptionPane.YES_NO_OPTION);
        if(option!=JOptionPane.YES_OPTION)
        {
            return;
        }
        notebook.closeCurrentResultSet();
        notebooks.remove(index);
        tabbedPane.remove(index);
        add(index);
        NotebookController newNotebook = notebooks.get(index);
        try
        {
            newNotebook.load(notebook.file);
            retitle();
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(
                    tabbedPane,
                    "Error loading: "+e.getMessage(),
                    "Error loading",
                    JOptionPane.ERROR_MESSAGE);
            notebooks.remove(index);
            tabbedPane.remove(index);
            notebooks.add(index,notebook);
            tabbedPane.add(notebook.notebookPanel,index);
            retitle();
            tabbedPane.setSelectedIndex(index);
        }
    }

    private void cloneTab()
    {
        try(var w = new StringWriter())
        {
            int index=tabbedPane.getSelectedIndex();
            notebooks.get(index).store(w);
            var notebook = add(-1);
            var r = new StringReader(w.getBuffer().toString());
            notebook.load(r);
        }
        catch(IOException ignored)
        {
            assert false;
        }
    }

    boolean isUnsaved()
    {
        return !notebooks.stream().noneMatch((n) -> n.isUnsaved()); 
    }
    
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
        
        item = new JMenuItem(openContainingFolderAction);
        item.setMnemonic(KeyEvent.VK_F);
        item.setEnabled(Desktop.isDesktopSupported());
        menu.add(item);

        item = new JMenuItem(saveAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_S,CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_S);
        menu.add(item);

        item = new JMenuItem(saveAsAction);
        item.setMnemonic(KeyEvent.VK_A);
        menu.add(item);

        item = new JMenuItem(renameAction);
        item.setMnemonic(KeyEvent.VK_M);
        menu.add(item);

        item = new JMenuItem(revertAction);
        item.setMnemonic(KeyEvent.VK_V);
        menu.add(item);

        item = new JMenuItem(cloneAction);
        item.setMnemonic(KeyEvent.VK_L);
        menu.add(item);

        item = new JMenuItem(closeAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_W,CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_C);
        menu.add(item);

        item = new JMenuItem(setWorkspaceAction);
        item.setMnemonic(KeyEvent.VK_W);
        menu.add(item);

        item = new JMenuItem(openPropertiesAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_COMMA,CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_P);
        item.setEnabled(propertyHolder.propertiesfiles.length > 0 &&
                Desktop.isDesktopSupported());
        menu.add(item);

        item = new JMenuItem(refreshPropertiesAction);
        item.setMnemonic(KeyEvent.VK_R);
        item.setEnabled(propertyHolder.propertiesfiles.length > 0);
        menu.add(item);

        menubar.add(menu);

        menu = new JMenu("Connection");
        menu.setMnemonic(KeyEvent.VK_C);

        item = new JMenuItem(commitAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_F7,0));
        item.setMnemonic(KeyEvent.VK_C);
        menu.add(item);
        
        item = new JMenuItem(rollbackAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_F9,0));
        item.setMnemonic(KeyEvent.VK_R);
        menu.add(item);
        
        item = new JMenuItem(openAction);
        item.setMnemonic(KeyEvent.VK_O);
        menu.add(item);

        item = new JMenuItem(disconnectAction);
        item.setMnemonic(KeyEvent.VK_D);
        menu.add(item);
        
        menubar.add(menu);

        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        
        item = new JMenuItem(new HelpAction(parent));
        item.setMnemonic(KeyEvent.VK_R);
        menu.add(item);
        
        item = new JMenuItem(new ShowSampleConfigAction(parent));
        item.setMnemonic(KeyEvent.VK_C);
        menu.add(item);
        
        menubar.add(menu);
        
        parent.setJMenuBar(menubar);
    }
    
    void restoreWorkspace()
    throws IOException
    {
        Objects.requireNonNull(workspaceFile);
        Workspace w = Workspaces.load(workspaceFile);
        
        if(w.getRecentFiles() != null)
        {
            for(var path : w.getRecentFiles()) recents.add(path);
        }
        
        if(w.getFiles().length==0) add(-1);
        int selectedIndex=0;
        for(String fn : w.getFiles())
        {
            File sqlFile = new File(fn);
            try
            {
                NotebookController notebook = add(-1);
                notebook.load(sqlFile);
                int index = tabbedPane.getSelectedIndex();
                if(fn.equals(w.getActiveFile())) selectedIndex = index;
                retitle();
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
        tabbedPane.setSelectedIndex(selectedIndex);
    }
    
    void saveWorkspace()
    throws IOException
    {
        if(workspaceFile==null) return;

        Workspace w = new Workspace();
        w.setFiles(notebooks
            .stream()
            .map((n) -> n.file)
            .filter(Objects::nonNull)
            .map((f) -> f.getPath())
            .toArray(String[]::new));
        var selectedNb = notebooks.get(tabbedPane.getSelectedIndex());
        if(selectedNb.file != null) w.setActiveFile(selectedNb.file.getPath());
        w.setRecentFiles(recents.toArray(new String[recents.size()]));
        Workspaces.store(w,workspaceFile);
    }
        
    NotebookController.Listener notebookListener = 
            new NotebookController.Listener()
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
            markChanged(selectedIndex);
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
        switch(e.getKeyCode())
        {
        case KeyEvent.VK_ENTER:
            notebooks.get(tabbedPane.getSelectedIndex()).restoreFocus();
        }
    }
    
    private int lastClicked=-1;

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if(tabbedPane.getSelectedIndex() == lastClicked) return;
        lastClicked = tabbedPane.getSelectedIndex();
        if(!tabbedPane.hasFocus())
        {
            NotebookController notebook = notebooks.get(lastClicked);
            if(notebook.hasSavedFocusPosition) notebook.restoreFocus();
        }
    }

    private void retitle()
    {
        for(int i=0;i<tabbedPane.getTabCount();i++)
        {
            File file = notebooks.get(i).file;
            String name = file!=null? file.getName() : "Untitled";
            logger.log(Level.FINE,"Name is <{0}>",name);
            String title=(i+1)+". "+name;
            logger.log(Level.FINE,"Tab title is <{0}>",title);
            tabbedPane.setTitleAt(i,title);
            if(notebooks.get(i).isUnsaved()) markChanged(i);
        }
    }

    private void markChanged(int index)
    {
        String title = tabbedPane.getTitleAt(index);
        if(!title.startsWith("*")) tabbedPane.setTitleAt(index,"*"+title);
    }
}
