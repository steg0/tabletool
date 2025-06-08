package de.steg0.deskapps.tabletool;

import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

class BufferResultSetPopup
{
    private JFrame parent;
    private BufferController b;

    BufferResultSetPopup(JFrame parent,BufferController b)
    {
        this.parent = parent;
        this.b=b;
    }

    void attach()
    {
        var popup = new JPopupMenu();
        JMenuItem item;
        item = new JMenuItem("Open as HTML",KeyEvent.VK_H);
        item.addActionListener((e) -> openResultAsHtml(false));
        popup.add(item);
        item = new JMenuItem("Open as HTML (transposed)",KeyEvent.VK_T);
        item.addActionListener((e) -> openResultAsHtml(true));
        popup.add(item);
        item = new JMenuItem("Open as CSV",KeyEvent.VK_V);
        item.addActionListener((e) -> openResultAsCsv());
        popup.add(item);
        item = new JMenuItem("Close",KeyEvent.VK_C);
        item.addActionListener((e) -> b.closeBuffer());
        popup.add(item);
        var popuplistener = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (e.isPopupTrigger()) popup.show(e.getComponent(),
                        e.getX(),e.getY());
            }
            @Override
            public void mouseReleased(MouseEvent e)
            {
                mousePressed(e);
            }
        };
        b.resultview.addMouseListener(popuplistener);
        b.resultview.getTableHeader().addMouseListener(popuplistener);
    }

    private void openResultAsHtml(boolean transposed)
    {
        try(var exporter = new DesktopExporter("tthtml",".html");
            var writer = new OutputStreamWriter(exporter.getOutputStream()))
        {
            var htmlbuf = new StringBuilder();
            htmlbuf.append("<pre>");
            b.editor.getText().chars().forEach((c) -> 
            {
                htmlbuf.append(HtmlEscaper.nonAscii(c));
            });
            htmlbuf.append("</pre>");
            writer.write(htmlbuf.toString());
            if(transposed)
            {
                b.getResultSetTableModel().toHtmlTransposed(writer);
            }
            else
            {
                writer.write(b.getResultSetTableModel().toHtml());
            }
            exporter.openWithDesktop();
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(
                    parent,
                    "Error exporting to file: "+e.getMessage(),
                    "Error exporting",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openResultAsCsv()
    {
        var sw = new StringWriter();
        try
        {
            b.getResultSetTableModel().store(sw,false);
        }
        catch(IOException e)
        {
            assert false: e.getMessage();
        }
        CsvExporter.openTemp(parent,sw.toString());
    }
}
