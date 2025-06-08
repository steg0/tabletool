package de.steg0.deskapps.tabletool;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class DesktopExporter implements AutoCloseable
{
    private File tmpfile;
    private OutputStream o;

    DesktopExporter(String prefix,String suffix) throws IOException
    {
        tmpfile = File.createTempFile(prefix,suffix);
        tmpfile.deleteOnExit();
        o = new FileOutputStream(tmpfile);
    }

    OutputStream getOutputStream()
    {
        return o;
    }

    void openWithDesktop() throws Exception
    {
        o.flush();
        Desktop.getDesktop().open(tmpfile);
    }

    @Override
    public void close() throws IOException
    {
        o.close();
    }
}
