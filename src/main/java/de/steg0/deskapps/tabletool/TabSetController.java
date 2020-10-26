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
        int count = tabbedPane.getComponentCount();
        tabbedPane.add("Notebook"+count,notebook.notebookPanel);
    }
    
    interface Actions
    {
        void add();
    }

    Actions actions = new Actions()
    {
        @Override
        public void add()
        {
            TabSetController.this.add();
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
        }
    }
}
