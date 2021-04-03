package de.steg0.deskapps.tabletool;

import java.awt.Component;
import java.util.Enumeration;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

class TableSizer
{
    /**
     * Sets preferred width for all columns in the table according to
     * their renderer preferences. This is taken from mergetool; comments
     * see there.
     */
    /* 
     * RS 2020-11-02: I think this could result in excessively wide columns
     * for some results. If so, a maximum width setting might be a good idea.
     * But let's first see whether JTable copes with it in some usable way
     * or not.
     */
    static void sizeColumns(JTable t)
    {
        int c=0;
        for(final Enumeration<TableColumn> e=
            t.getColumnModel().getColumns();e.hasMoreElements();c++)
        {
            TableColumn col=e.nextElement();
            int mWidth=col.getMinWidth();
            int maxWidth=col.getMaxWidth();
            TableCellRenderer hRenderer = col.getHeaderRenderer();
            if(hRenderer==null) hRenderer=t.getTableHeader()
                    .getDefaultRenderer();
            Component hComp = hRenderer.getTableCellRendererComponent(
                    t,col.getHeaderValue(),false,false,-1,c);
            mWidth=Math.max(mWidth,hComp.getPreferredSize().width);
            for(int i=0;i<t.getRowCount();i++)
            {
                TableCellRenderer renderer=t.getCellRenderer(i,c);
                Component comp=t.prepareRenderer(renderer,i,c);
                int compPreferredSize=comp.getPreferredSize().width +
                        t.getIntercellSpacing().width;
                mWidth=Math.max(mWidth,compPreferredSize);
            }
            if(mWidth >= maxWidth)
            {
                mWidth = maxWidth;
            }
            col.setPreferredWidth(mWidth);
        }
    }
}
