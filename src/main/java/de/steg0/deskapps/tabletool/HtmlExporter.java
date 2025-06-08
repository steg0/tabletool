package de.steg0.deskapps.tabletool;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

class HtmlExporter implements AutoCloseable
{
    private File tmpfile;
    private Writer w;

    HtmlExporter() throws IOException
    {
        tmpfile = File.createTempFile("tthtml",".html");
        tmpfile.deleteOnExit();
        w = new OutputStreamWriter(new FileOutputStream(tmpfile));
    }

    Writer getWriter()
    {
        return w;
    }

    void openWithDesktop() throws Exception
    {
        w.flush();
        Desktop.getDesktop().open(tmpfile);
    }

    @Override
    public void close() throws IOException
    {
        w.close();
    }
}
