package de.steg0.deskapps.tabletool;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class HtmlExporter
{
    /**blocking */
    static void openTemp(JFrame parent,String html)
    {
        try
        {
            var tmpfile = File.createTempFile("tthtml",".html");
            tmpfile.deleteOnExit();
            try(var ow = new OutputStreamWriter(new FileOutputStream(tmpfile)))
            {
                ow.write(html);
            }
            Desktop.getDesktop().open(tmpfile);
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(
                    parent,
                    "Error exporting: "+e.getMessage(),
                    "Error exporting",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
