package de.steg0.deskapps.tabletool;

import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellEditor;

import de.steg0.deskapps.tabletool.Connections.ConnectionState;

class OpenConnectionDialogController
{
    Logger logger = Logger.getLogger("tabtype");

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
        table.setColumnSelectionAllowed(true);
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
        table.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent e)
            {
                if(e.getClickCount() == 2)
                    closeButtonListener.actionPerformed(null);
            }
        });
        table.addKeyListener(new OpenConnectionDialogKeyListener(this,hint));

        f.pack();
        f.setLocationRelativeTo(parent);
        
        table.setRowSelectionInterval(0,0);
        moveTo(table,hint);

        index = null;
        f.setVisible(true);

        if(index != null)
        {
            TableCellEditor editor = table.getCellEditor();
            if(editor!=null) editor.stopCellEditing();
            notebook.openConnection(index);
        }
    }

    void moveTo(JTable table,String hint)
    {
        logger.log(Level.FINE,"Looking for connection name \"{0}...\"",hint);
        for(int i=0;i<notebook.connections.getRowCount();i++)
        {
            ConnectionState val = notebook.connections.getElementAt(i);
            if(val.info().name.startsWith(hint) ||
               hint.length()>1 && val.info().name.contains(hint))
            {
                table.setRowSelectionInterval(i,i);
                table.scrollRectToVisible(table.getCellRect(i,0,true));
                break;
            }
        }
        table.setColumnSelectionInterval(0,0);
        table.requestFocusInWindow();
    }
}

class OpenConnectionDialogKeyListener extends KeyAdapter
{
    private long lastKeyTime = System.currentTimeMillis();
    private String searchstr;
    private OpenConnectionDialogController controller;

    OpenConnectionDialogKeyListener(OpenConnectionDialogController c,
            String searchstr)
    {
        this.searchstr = searchstr;
        controller = c;
    }

    private void updateSearchStr(long time,char character)
    {
        if(time-lastKeyTime < 700)
        {
            searchstr += character;
        }
        else
        {
            searchstr=String.valueOf(character);
        }

        lastKeyTime = time;
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
        char c = e.getKeyChar();
        if(e.getModifiersEx() != 0 && e.getModifiersEx() != SHIFT_DOWN_MASK)
            return;
        if(c != KeyEvent.CHAR_UNDEFINED && Character.isLetterOrDigit(c))
        {
            e.consume();
            updateSearchStr(e.getWhen(),c);
            controller.moveTo((JTable)e.getSource(),searchstr);
        }
    }
}
