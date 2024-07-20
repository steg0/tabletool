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
    static void sizeColumns(JTable t,int limit)
    {
        int c=0;
        for(final Enumeration<TableColumn> e=
            t.getColumnModel().getColumns();e.hasMoreElements();c++)
        {
            TableColumn col=e.nextElement();
            int colMinWidth=col.getMinWidth();
            int colMaxWidth=col.getMaxWidth();
            TableCellRenderer hRenderer = col.getHeaderRenderer();
            if(hRenderer==null) hRenderer=t.getTableHeader()
                    .getDefaultRenderer();
            Component hComp = hRenderer.getTableCellRendererComponent(
                    t,col.getHeaderValue(),false,false,-1,c);
            colMinWidth=Math.max(colMinWidth,hComp.getPreferredSize().width);
            for(int i=0;i<t.getRowCount();i++)
            {
                TableCellRenderer renderer=t.getCellRenderer(i,c);
                Component comp=t.prepareRenderer(renderer,i,c);
                int compPreferredSize=comp.getPreferredSize().width +
                        t.getIntercellSpacing().width;
                colMinWidth=Math.max(colMinWidth,compPreferredSize);
            }
            if(colMinWidth >= colMaxWidth)
            {
                colMinWidth = colMaxWidth;
            }
            if(colMinWidth >= limit)
            {
                colMinWidth = limit;
            }
            col.setPreferredWidth(colMinWidth);
        }
    }
}
