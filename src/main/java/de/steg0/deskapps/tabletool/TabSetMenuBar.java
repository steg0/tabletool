package de.steg0.deskapps.tabletool;

import static java.awt.event.ActionEvent.CTRL_MASK;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

class TabSetMenuBar
{
    private final JFrame parent;
    private final TabSetController tabset;
    private final PropertyHolder propertyHolder;

    TabSetMenuBar(JFrame parent,TabSetController tabset,
            PropertyHolder propertyHolder)
    {
        this.parent = parent;
        this.tabset = tabset;
        this.propertyHolder = propertyHolder;
    }
    
    void recreate()
    {
        var menubar = new JMenuBar();
        
        JMenu menu;
        
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        JMenuItem item;

        item = new JMenuItem(tabset.addAction);
        item.setMnemonic(KeyEvent.VK_N);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_T,CTRL_MASK));
        menu.add(item);
        
        item = new JMenuItem(tabset.loadAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_O,CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_O);
        menu.add(item);

        item = tabset.getRecentsMenu();
        menu.add(item);
        
        item = new JMenuItem(tabset.openContainingFolderAction);
        item.setMnemonic(KeyEvent.VK_F);
        item.setEnabled(Desktop.isDesktopSupported());
        menu.add(item);

        item = new JMenuItem(tabset.saveAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_S,CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_S);
        menu.add(item);

        item = new JMenuItem(tabset.saveAsAction);
        item.setMnemonic(KeyEvent.VK_A);
        menu.add(item);

        item = new JMenuItem(tabset.renameAction);
        item.setMnemonic(KeyEvent.VK_M);
        menu.add(item);

        item = new JMenuItem(tabset.revertAction);
        item.setMnemonic(KeyEvent.VK_V);
        menu.add(item);

        item = new JMenuItem(tabset.cloneAction);
        item.setMnemonic(KeyEvent.VK_L);
        menu.add(item);

        item = new JMenuItem(tabset.closeAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_W,CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_C);
        menu.add(item);

        item = new JMenuItem(tabset.setWorkspaceAction);
        item.setMnemonic(KeyEvent.VK_W);
        menu.add(item);

        item = new JMenuItem(tabset.openPropertiesAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_COMMA,CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_P);
        item.setEnabled(propertyHolder.propertiesfiles.length > 0 &&
                Desktop.isDesktopSupported());
        menu.add(item);

        item = new JMenuItem(tabset.refreshPropertiesAction);
        item.setMnemonic(KeyEvent.VK_R);
        item.setEnabled(propertyHolder.propertiesfiles.length > 0);
        menu.add(item);

        menubar.add(menu);

        menu = new JMenu("Connection");
        menu.setMnemonic(KeyEvent.VK_C);

        item = new JMenuItem(tabset.commitAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_F7,0));
        item.setMnemonic(KeyEvent.VK_C);
        menu.add(item);
        
        item = new JMenuItem(tabset.rollbackAction);
        item.setAccelerator(getKeyStroke(KeyEvent.VK_F9,0));
        item.setMnemonic(KeyEvent.VK_R);
        menu.add(item);
        
        item = new JMenuItem(tabset.openAction);
        item.setMnemonic(KeyEvent.VK_O);
        menu.add(item);

        item = new JMenuItem(tabset.disconnectAction);
        item.setMnemonic(KeyEvent.VK_D);
        menu.add(item);
        
        menubar.add(menu);

        var externalToolActions = getExternalToolActions();
        if(!externalToolActions.isEmpty())
        {
            menu = new JMenu("Tools");
            menu.setMnemonic(KeyEvent.VK_T);

            for(ExternalToolAction action : externalToolActions)
            {
                item = new JMenuItem(action);
                menu.add(item);
            }
            
            menubar.add(menu);
        }

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

    private List<ExternalToolAction> getExternalToolActions()
    {
        ExternalToolDefinition[] externalToolDefinitions = propertyHolder
                .getExternalToolDefinitions();
        List<ExternalToolAction> actions = new ArrayList<>();
        for(int i=0;i<externalToolDefinitions.length;i++)
        {
            actions.add(new ExternalToolAction(tabset,
                    externalToolDefinitions[i],i));
        }
        return actions;
    }
}
