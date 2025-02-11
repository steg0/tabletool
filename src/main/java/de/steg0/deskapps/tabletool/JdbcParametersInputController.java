package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.swing.BorderFactory;
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
            {"In","Numeric?","In Value","Out","Numeric?","Out Value"};

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
            cb.setBorderPainted(hasFocus);
            Color focusColor = closeButton.getForeground();
            cb.setBorder(BorderFactory.createLineBorder(focusColor));
            return cb;
        }
    };

    JdbcParametersInputController(JFrame parent)
    {
        this.parent = parent;
    }

    private void createTable()
    {
        table = new JTable(data,COLUMN_HEADERS);
        table.getColumnModel().getColumn(0).setCellEditor(cbEditor);
        table.getColumnModel().getColumn(1).setCellEditor(cbEditor);
        table.getColumnModel().getColumn(3).setCellEditor(cbEditor);
        table.getColumnModel().getColumn(4).setCellEditor(cbEditor);
        table.getColumnModel().getColumn(0).setCellRenderer(cbRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(cbRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(cbRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(cbRenderer);
        tablepane = new JScrollPane(table);
    }

    private void initGrid()
    {
        var f = new JDialog(parent,"JDBC Parameters Input",false);
        f.setLocationRelativeTo(parent);
        f.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        ((BorderLayout)f.getContentPane().getLayout()).setVgap(5);

        var explanation = new JTextArea("Please configure " +
                "JDBC parameters to set in the query.\n" +
                "Either varchar or number values are supported.\n" +
                "Use Enter to accept a value in a cell.");
        explanation.setEditable(false);
        f.getContentPane().add(explanation,BorderLayout.NORTH);
        
        data = new Object[9][6];
        createTable();
        f.getContentPane().add(tablepane);

        var buttonPanel = new JPanel();

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
        Object[][] newData = new Object[data.length+1][6];
        System.arraycopy(data,0,newData,0,data.length);
        data = newData;
        dialog.getContentPane().remove(tablepane);
        createTable();
        dialog.getContentPane().add(tablepane);
        dialog.revalidate();
    }

    void remove()
    {
        if(data.length==0) return;
        Object[][] newData = new Object[data.length-1][6];
        System.arraycopy(data,0,newData,0,newData.length);
        data = newData;
        dialog.getContentPane().remove(tablepane);
        createTable();
        dialog.getContentPane().add(tablepane);
        dialog.revalidate();        
    }

    void setVisible(boolean visible)
    {
        if(dialog==null) initGrid();
        dialog.setVisible(visible);
        if(visible) table.requestFocusInWindow();
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
        if(dialog==null) initGrid();
        for(int i=0;i<table.getRowCount();i++)
        {
            boolean in = Boolean.TRUE.equals(table.getModel().getValueAt(i,0));
            boolean inNumeric = Boolean.TRUE.equals(
                    table.getModel().getValueAt(i,1));
            boolean out = Boolean.TRUE.equals(
                    table.getModel().getValueAt(i,3));
            boolean outNumeric = Boolean.TRUE.equals(
                    table.getModel().getValueAt(i,4));
                if(out && stmt instanceof CallableStatement cstmt)
            {
                if(outNumeric)
                {
                    cstmt.registerOutParameter(i+1,JDBCType.DECIMAL);
                }
                else
                {
                    cstmt.registerOutParameter(i+1,JDBCType.VARCHAR);
                }
            }
            if(in)
            {
                Object value = table.getModel().getValueAt(i,2);
                boolean setNull = value == null;
                if(inNumeric)
                {        
                    stmt.setBigDecimal(i+1,setNull?null:new BigDecimal(
                            value.toString()));
                }
                else
                {
                    stmt.setString(i+1,setNull?null:value.toString());
                }
            }
        }
    }

    void readFromStatement(PreparedStatement stmt)
    throws SQLException
    {
        if(dialog==null) initGrid();
        for(int i=0;i<table.getRowCount();i++)
        {
            boolean out = Boolean.TRUE.equals(
                table.getModel().getValueAt(i,3));
            boolean outNumeric = Boolean.TRUE.equals(
                    table.getModel().getValueAt(i,4));
            if(out && stmt instanceof CallableStatement cstmt)
            {
                Object value = outNumeric?
                        cstmt.getBigDecimal(i+1) : cstmt.getString(i+1);
                table.getModel().setValueAt(
                        value==null?null:value.toString(),i,5);
            }
        }
    }
}