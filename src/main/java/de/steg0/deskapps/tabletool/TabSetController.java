package de.steg0.deskapps.tabletool;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

class TabSetController
implements KeyListener
{
    JFrame parent;
    PropertyHolder propertyHolder;
    
    ConnectionListModel connections;
    Executor executor = Executors.newCachedThreadPool();

    JTabbedPane tabbedPane = new JTabbedPane();
    List<JdbcNotebookController> notebooks = new ArrayList<>();
    int unnamedNotebookCount;
    
    TabSetController(JFrame parent,PropertyHolder propertyHolder)
    {
        this.parent = parent;
        this.propertyHolder = propertyHolder;
        
        connections = new ConnectionListModel(propertyHolder,executor);
        
        this.tabbedPane.addKeyListener(this);
        
        add();
    }
    
    void add()
    {
        var notebook = new JdbcNotebookController(
                parent,
                propertyHolder,
                connections,
                actions
        );
        notebooks.add(notebook);
        String newname = "Notebook"+(unnamedNotebookCount++);
        tabbedPane.add(newname,notebook.notebookPanel);
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
        void reportDisconnect(ConnectionWorker connection);
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
