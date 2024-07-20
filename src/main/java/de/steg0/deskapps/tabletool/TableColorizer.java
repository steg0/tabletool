package de.steg0.deskapps.tabletool;

import java.awt.Color;

import javax.swing.JTable;

class TableColorizer
{
    /**Taints table/table header background a bit (null safe). */
    static void colorize(JTable table,Color bg)
    {
        if(table==null) return;
        int r = bg.getRed(),
            g = bg.getGreen(),
            b = bg.getBlue(),
            rb = r + (0xff-r)/4,
            gb = g + (0xff-g)/4,
            bb = b + (0xff-b)/4,
            rd = r - r/4,
            gd = g - g/4,
            bd = b - b/4;
        Color
            brighter = new Color(rb, gb, bb),
            darker = new Color(rd, gd, bd);
        if(g >= 0x80)
        {
            table.setBackground(brighter);
            table.getTableHeader().setBackground(darker);
        }
        else
        {
            table.setBackground(darker);
            table.getTableHeader().setBackground(brighter);
        }
    }
}