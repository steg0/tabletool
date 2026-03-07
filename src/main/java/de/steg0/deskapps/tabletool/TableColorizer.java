package de.steg0.deskapps.tabletool;

import java.awt.Color;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;

class TableColorizer
{
    /**
     * Taints table/table header background a bit (null safe). This doesn't work
     * well for some LAFs. It is only activated for the Metal LAF.
    */
    static void colorize(JTable table,Color bg,BufferConfigSource cfg)
    {
        if(!(UIManager.getLookAndFeel().getClass().getName().equals(
                UIManager.getCrossPlatformLookAndFeelClassName()) &&
                cfg.colorizeJavaLafTables())) return;
        if(table==null) return;
        JScrollPane scrollpane = (JScrollPane)table.getParent().getParent();
        int r = bg.getRed(),
            g = bg.getGreen(),
            b = bg.getBlue(),
            rb = r + (0xff-r)/5,
            gb = g + (0xff-g)/5,
            bb = b + (0xff-b)/5,
            rd = r - r/5,
            gd = g - g/5,
            bd = b - b/5;
        Color
            brighter = new Color(rb, gb, bb),
            darker = new Color(rd, gd, bd);
        if(g >= 0x80)
        {
            table.setBackground(brighter);
            table.getTableHeader().setBackground(darker);
            scrollpane.setBackground(darker);
            scrollpane.getVerticalScrollBar().setBackground(brighter);
        }
        else
        {
            table.setBackground(darker);
            table.getTableHeader().setBackground(brighter);
            scrollpane.setBackground(brighter);
            scrollpane.getVerticalScrollBar().setBackground(darker);
        }
    }
}