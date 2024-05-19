package de.steg0.deskapps.tabletool;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTable;

class TableFontSizer
{
    static Logger logger = Logger.getLogger("tabletool");
    
    static void setFontSize(JTable resultview,int newSize,int numlines)
    {
        if(resultview==null) return;
        Font rf = resultview.getFont(),
             rf2 = new Font(rf.getName(),rf.getStyle(),newSize);
        resultview.setFont(rf2);
        var header = resultview.getTableHeader();
        Font hf = header.getFont(),
             hf2 = new Font(hf.getName(),hf.getStyle(),newSize);
        resultview.getTableHeader().setFont(hf2);
        int lineHeight = (int)hf2.getMaxCharBounds(new FontRenderContext(
                null,false,false)).getHeight();
        TableSizer.sizeColumns(resultview);
        resultview.setRowHeight(lineHeight);
        Dimension preferredSize = resultview.getPreferredSize();
        var viewportSize = new Dimension((int)preferredSize.getWidth(),
                (int)Math.min(numlines*lineHeight,preferredSize.getHeight()));
        logger.log(Level.FINE,"Sizing table, viewportSize={0}, "+
                "lineHeight={1}",new Object[]{viewportSize,lineHeight});
        resultview.setPreferredScrollableViewportSize(viewportSize);
    }
}
