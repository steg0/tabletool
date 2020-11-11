package de.steg0.deskapps.tabletool;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

class HtmlSelection implements Transferable
{
    String html;

    HtmlSelection(String html)
    {
        this.html = html;
    }

    public DataFlavor[] getTransferDataFlavors()
    {
        return new DataFlavor[]{DataFlavor.fragmentHtmlFlavor};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        return flavor.equals(DataFlavor.fragmentHtmlFlavor);
    }

    public Object getTransferData(DataFlavor flavor)
    throws UnsupportedFlavorException
    {
        if(String.class.equals(flavor.getRepresentationClass()))
        {
            return html;
        }
        throw new UnsupportedFlavorException(flavor);
    }
}
