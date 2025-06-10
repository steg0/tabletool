package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellEditor;

class OpenConnectionDialogController
{
    private NotebookController notebook;
    private JFrame parent;
    private Integer index;

    OpenConnectionDialogController(NotebookController notebook,
            JFrame parent)
    {
        this.notebook = notebook;
        this.parent = parent;
    }

    void pick(String hint)
    {
        var f = new JDialog(parent,"Connection Selection",true);
        f.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        ((BorderLayout)f.getContentPane().getLayout()).setVgap(5);
        
        var explanation = new JTextArea("Available sources are listed " +
                "below. Adjustments won't affect established connections " +
                "and are not persistent.");
        explanation.setEditable(false);
        f.getContentPane().add(explanation,BorderLayout.NORTH);
        
        var table = new JTable(notebook.connections);
        TableSizer.sizeColumns(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        var tablepane = new JScrollPane(table);
        f.getContentPane().add(tablepane);
        
        var closeButton = new JButton("Select");
        ActionListener closeButtonListener = e ->
        {
            index = table.getSelectedRow();
            f.dispose();
        };
        closeButton.addActionListener(closeButtonListener);
        f.getContentPane().add(closeButton,BorderLayout.SOUTH);
        
        f.getRootPane().setDefaultButton(closeButton);
        f.getRootPane().registerKeyboardAction(evt -> f.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        table.registerKeyboardAction(closeButtonListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        f.pack();
        f.setLocationRelativeTo(parent);
        
        table.setRowSelectionInterval(0,0);
        for(int i=0;i<notebook.connections.getRowCount();i++)
        {
            Object val = notebook.connections.getValueAt(i,0);
            if(val != null && val.toString().contains(hint))
            {
                table.setRowSelectionInterval(i,i);
                break;
            }
        }
        table.setColumnSelectionInterval(0,0);
        table.requestFocusInWindow();

        index = null;
        f.setVisible(true);

        if(index != null)
        {
            TableCellEditor editor = table.getCellEditor();
            if(editor!=null) editor.stopCellEditing();
            notebook.openConnection(index);
        }
    }
}