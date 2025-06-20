package de.steg0.deskapps.tabletool;

import static java.lang.Boolean.TRUE;

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

    private final JFrame dialog;
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
            cb.setSelected(TRUE.equals(value));
            cb.setBorderPainted(hasFocus);
            Color focusColor = closeButton.getForeground();
            cb.setBorder(BorderFactory.createLineBorder(focusColor));
            return cb;
        }
    };

    JdbcParametersInputController(JFrame dialog)
    {
        this.dialog = dialog;
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
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        var layout = new BorderLayout(5,5);
        dialog.getContentPane().setLayout(layout);

        var explanation = new JTextArea("JDBC parameters entered here will " +
                "be applied to the query\n" +
                "while this window is open.\n" +
                "Either varchar or number values are supported.\n" +
                "Use Enter to accept a value in a cell.");
        explanation.setEditable(false);
        dialog.getContentPane().add(explanation,BorderLayout.NORTH);
        
        data = new Object[9][6];
        createTable();
        dialog.getContentPane().add(tablepane);

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

        dialog.getContentPane().add(buttonPanel,BorderLayout.SOUTH);

        dialog.getRootPane().registerKeyboardAction(
                evt -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setPreferredSize(new Dimension(400,300));
        dialog.pack();
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
        if(table==null) initGrid();
        dialog.setVisible(visible);
        if(visible)
        {
            if(table.isEditing()) table.getCellEditor().stopCellEditing();
            table.setRowSelectionInterval(0,0);
            table.setColumnSelectionInterval(0,0);
            table.requestFocusInWindow();
        }
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

    String applyToStatement(PreparedStatement stmt)
    throws SQLException
    {
        if(table==null) initGrid();
        if(!dialog.isVisible()) return "";
        for(int i=0;i<table.getRowCount();i++)
        {
            boolean in = TRUE.equals(table.getModel().getValueAt(i,0));
            boolean inNumeric = TRUE.equals(table.getModel().getValueAt(i,1));
            boolean out = TRUE.equals(table.getModel().getValueAt(i,3));
            boolean outNumeric = TRUE.equals(table.getModel().getValueAt(i,4));
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
        }
        return describeInValues();
    }

    String readFromStatement(PreparedStatement stmt)
    throws SQLException
    {
        if(table==null) initGrid();
        for(int i=0;i<table.getRowCount();i++)
        {
            boolean out = TRUE.equals(table.getModel().getValueAt(i,3));
            boolean outNumeric = TRUE.equals(table.getModel().getValueAt(i,4));
            if(out && stmt instanceof CallableStatement cstmt)
            {
                Object value = outNumeric?
                        cstmt.getBigDecimal(i+1) : cstmt.getString(i+1);
                table.getModel().setValueAt(
                        value==null?null:value.toString(),i,5);
            }
        }
        return describeOutValues();
    }

    private String describeInValues()
    {
        var b = new StringBuilder();
        for(int i=0;i<table.getRowCount();i++)
        {
            var data = table.getModel();
            boolean in = TRUE.equals(data.getValueAt(i,0));
            if(in)
            {
                if(i>0) b.append(", ");
                b.append("?");
                b.append(Integer.toString(i+1));
                b.append(":");
                boolean numeric = TRUE.equals(data.getValueAt(i,1));
                Object val = data.getValueAt(i,2);
                if(!numeric&&val!=null) b.append("\"");
                String s = String.valueOf(val);
                b.append(s.replace("\n","\\n").replace("\"","\\\""));
                if(!numeric&&val!=null) b.append("\"");
            }
        }
        return b.toString();
    }

    private String describeOutValues()
    {
        var b = new StringBuilder();
        for(int i=0;i<table.getRowCount();i++)
        {
            var data = table.getModel();
            boolean out = TRUE.equals(data.getValueAt(i,3));
            if(out)
            {
                if(i>0) b.append(", ");
                b.append("?");
                b.append(Integer.toString(i+1));
                b.append(":");
                boolean numeric = TRUE.equals(data.getValueAt(i,4));
                Object val = data.getValueAt(i,5);
                if(!numeric&&val!=null) b.append("\"");
                String s = String.valueOf(val);
                b.append(s.replace("\n","\\n").replace("\"","\\\""));
                if(!numeric&&val!=null) b.append("\"");
            }
        }
        if(b.length()>0) return " -> " + b.toString();
        return "";
    }
}