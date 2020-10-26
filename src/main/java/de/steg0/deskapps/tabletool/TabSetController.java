package de.steg0.deskapps.tabletool;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

class TabSetController
implements KeyListener
{
    JFrame parent;
    PropertyHolder propertyHolder;
    JTabbedPane tabbedPane = new JTabbedPane();
    List<JdbcNotebookController> notebooks = new ArrayList<>();
    int unnamedNotebookCount;
    
    TabSetController(JFrame parent,PropertyHolder propertyHolder)
    {
        this.parent = parent;
        this.propertyHolder = propertyHolder;
        
        this.tabbedPane.addKeyListener(this);
        
        add();
    }
    
    void add()
    {
        var notebook = new JdbcNotebookController(parent,propertyHolder,actions);
        notebooks.add(notebook);
        tabbedPane.add("Notebook"+(unnamedNotebookCount++),notebook.notebookPanel);
    }
    
    void remove(JdbcNotebookController notebook)
    {
        notebooks.remove(notebook);
        for(var c : tabbedPane.getComponents())
        {
            if(c==notebook.notebookPanel)
            {
                tabbedPane.remove(c);
                return;
            }
        }
    }
    
    void removeSelected()
    {
        notebooks.remove(tabbedPane.getSelectedIndex());
        tabbedPane.remove(tabbedPane.getSelectedIndex());
    }
    
    interface Actions
    {
        void add();
        void removeSelected();
    }

    Actions actions = new Actions()
    {
        @Override
        public void add()
        {
            TabSetController.this.add();
        }
        @Override
        public void removeSelected()
        {
            TabSetController.this.removeSelected();
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
            if(e.isControlDown()) this.add();
            break;
        case KeyEvent.VK_W:
            if(e.isControlDown()) this.removeSelected();
        }
    }
}
