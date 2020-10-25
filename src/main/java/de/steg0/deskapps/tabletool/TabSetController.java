package de.steg0.deskapps.tabletool;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

class TabSetController
{
    JFrame parent;
    PropertyHolder propertyHolder;
    JTabbedPane tabbedPane = new JTabbedPane();
    
    TabSetController(JFrame parent,PropertyHolder propertyHolder)
    {
        this.parent = parent;
        this.propertyHolder = propertyHolder;
        
        add();
    }
    
    void add()
    {
        var notebook = new JdbcNotebookController(parent,propertyHolder,actions);
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
}
