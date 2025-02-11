package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.sql.CallableStatement;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

class JdbcParametersInputController implements ActionListener
{
    private static final String[] COLUMN_HEADERS =
            {"In","In Value","Out","Out Value"};

    private JFrame parent;
    private JDialog dialog;
    private JTable table;
    private JScrollPane tablepane;
    private Object[][] data;
    private JButton addButton;
    private JButton removeButton;
    private JButton closeButton;
    private TableCellEditor cbEditor = new DefaultCellEditor(new JCheckBox());
    private TableCellRenderer cbRenderer = new TableCellRenderer()
    {
        private JCheckBox cb = new JCheckBox();
        @Override 
        public Component getTableCellRendererComponent(JTable table,
                Object value,boolean isSelected,boolean hasFocus,int row,
                int column)
        {
            cb.setSelected(Boolean.TRUE.equals(value));
            return cb;
        }
    };

    JdbcParametersInputController(JFrame parent)
    {
        this.parent = parent;
        initGrid();
    }

    void initGrid()
    {
        var f = new JDialog(parent,"JDBC Parameters Input",false);
        f.setLocationRelativeTo(parent);
        f.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        ((BorderLayout)f.getContentPane().getLayout()).setVgap(5);

        var explanation = new JTextArea("Please configure " +
                "JDBC parameters to set in the query.\n" +
                "Use Enter to accept a value in a cell.");
        explanation.setEditable(false);
        f.getContentPane().add(explanation,BorderLayout.NORTH);
        
        data = new Object[10][4];
        table = new JTable(data,COLUMN_HEADERS);
        table.getColumnModel().getColumn(0).setCellEditor(cbEditor);
        table.getColumnModel().getColumn(2).setCellEditor(cbEditor);
        table.getColumnModel().getColumn(0).setCellRenderer(cbRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(cbRenderer);
        tablepane = new JScrollPane(table);
        f.getContentPane().add(tablepane);

        var buttonPanel = new JPanel(new GridLayout(1,3));

        addButton = new JButton("Append New");
        addButton.setMnemonic(KeyEvent.VK_A);
        addButton.addActionListener(this);
        buttonPanel.add(addButton);

        removeButton = new JButton("Remove Last");
        removeButton.setMnemonic(KeyEvent.VK_R);
        removeButton.addActionListener(this);
        buttonPanel.add(removeButton);

        closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        buttonPanel.add(closeButton);

        f.getContentPane().add(buttonPanel,BorderLayout.SOUTH);

        f.getRootPane().registerKeyboardAction(
                evt -> f.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        f.setPreferredSize(new Dimension(400,300));
        f.pack();
        dialog = f;
    }

    void add()
    {
        Object[][] newData = new Object[data.length+1][4];
        System.arraycopy(data,0,newData,0,data.length);
        data = newData;
        dialog.getContentPane().remove(tablepane);
        table = new JTable(data,COLUMN_HEADERS);
        table.getColumnModel().getColumn(0).setCellEditor(cbEditor);
        table.getColumnModel().getColumn(2).setCellEditor(cbEditor);
        table.getColumnModel().getColumn(0).setCellRenderer(cbRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(cbRenderer);
        tablepane = new JScrollPane(table);
        dialog.getContentPane().add(tablepane);
        dialog.revalidate();
    }

    void remove()
    {
        if(data.length==0) return;
        Object[][] newData = new Object[data.length-1][4];
        System.arraycopy(data,0,newData,0,newData.length);
        data = newData;
        dialog.getContentPane().remove(tablepane);
        table = new JTable(data,COLUMN_HEADERS);
        table.getColumnModel().getColumn(0).setCellEditor(cbEditor);
        table.getColumnModel().getColumn(2).setCellEditor(cbEditor);
        table.getColumnModel().getColumn(0).setCellRenderer(cbRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(cbRenderer);
        tablepane = new JScrollPane(table);
        dialog.getContentPane().add(tablepane);
        dialog.revalidate();        
    }

    void setVisible(boolean visible)
    {
        dialog.setVisible(visible);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == addButton)
        {
            add();
        }
        else if(e.getSource() == removeButton)
        {
            remove();
        }
        else if(e.getSource() == closeButton)
        {
            setVisible(false);
        }
    }

    void applyToStatement(PreparedStatement stmt)
    throws SQLException
    {
        if(!dialog.isVisible()) return;
        for(int i=0;i<table.getRowCount();i++)
        {
            boolean in = Boolean.TRUE.equals(table.getModel().getValueAt(i,0));
            boolean out = Boolean.TRUE.equals(
                    table.getModel().getValueAt(i,2));
            if(out && stmt instanceof CallableStatement cstmt)
            {
                cstmt.registerOutParameter(i+1,JDBCType.OTHER);
            }
            if(in)
            {
                stmt.setObject(i+1,table.getModel().getValueAt(i,1));
            }
        }
    }

    void readFromStatement(PreparedStatement stmt)
    throws SQLException
    {
        if(!dialog.isVisible()) return;
        for(int i=0;i<table.getRowCount();i++)
        {
            boolean out = Boolean.TRUE.equals(
                table.getModel().getValueAt(i,2));
            if(out && stmt instanceof CallableStatement cstmt)
            {
                table.getModel().setValueAt(cstmt.getObject(i+1),i,3);
            }
        }
    }
}