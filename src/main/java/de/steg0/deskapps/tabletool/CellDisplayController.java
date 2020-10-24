package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.Window;
import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Clob;
import java.sql.SQLException;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

class CellDisplayController
{
    JPanel panel = new JPanel(new BorderLayout());
    Window parent;
    Object value;
    
    CellDisplayController(Window parent,Object val)
    {
        this.parent = parent;
        value = val;
    }
    
    void show()
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
