package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Date;
import java.util.function.Consumer;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

class CellDisplayController
{
    JPanel panel = new JPanel(new BorderLayout());
    Window parent;
    
    CellDisplayController(Window parent,JTable source,Consumer<String> log)
    {
        this.parent = parent;
        
        source.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                if(event.getClickCount() == 2)
                {
                    int row = source.rowAtPoint(event.getPoint()),
                        col = source.columnAtPoint(event.getPoint());
                    Object cellcontent = source.getValueAt(row,col);
                    try
                    {
                        show(cellcontent);
                    }
                    catch(SQLException e)
                    {
                        log.accept(SQLExceptionPrinter.toString(e));
                    }
                    catch(IOException e)
                    {
                        StringBuilder b=new StringBuilder();
                        b.append("IOException occured at ");
                        b.append(new Date());
                        b.append(":\n");
                        b.append(e.getMessage());
                        log.accept(b.toString());
                    }
                }               
            }
        });
    }
    
    void show(Object value)
    throws SQLException,IOException
    {
        var dialog = new JDialog(parent,"Cell display");
        
        dialog.getContentPane().setLayout(new BorderLayout());
        
        var textarea = new JTextArea(8,60);
        var scrollpane = new JScrollPane(textarea);
        if(value instanceof Clob)
        {
            var b = new StringBuilder();
            var l = new LineNumberReader(((Clob)value).getCharacterStream());
            for(String line=l.readLine();line!=null;line=l.readLine())
            {
                b.append(line);
            }
            textarea.setText(b.toString());
        }
        else
        {
            textarea.setText(value.toString());
        }
        dialog.getContentPane().add(scrollpane);
        
        dialog.pack();
        dialog.setVisible(true);
    }
}
