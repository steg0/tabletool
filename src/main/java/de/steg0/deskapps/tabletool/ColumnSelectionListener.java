package de.steg0.deskapps.tabletool;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.JTable;

class ColumnSelectionListener
{
    private JTable t;

    ColumnSelectionListener(JTable t)
    {
        this.t=t;
    }

    void attach()
    {
        var listener = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(e.getButton() != MouseEvent.BUTTON1) return;
                int clickcolumn = t.columnAtPoint(e.getPoint());

                if(e.isShiftDown())
                {
                    var stat = Arrays.stream(t.getSelectedColumns())
                            .summaryStatistics();
                    var min = Math.min(clickcolumn,stat.getMin());
                    var max = Math.max(clickcolumn,stat.getMax());
                    t.clearSelection();
                    t.addRowSelectionInterval(0,t.getRowCount()-1);
                    t.addColumnSelectionInterval(min,max);
                }
                else if(e.isControlDown())
                {
                    if(t.isColumnSelected(clickcolumn))
                    {
                        t.removeColumnSelectionInterval(clickcolumn,
                                clickcolumn);
                    }
                    else
                    {
                        t.addRowSelectionInterval(0,t.getRowCount()-1);
                        t.addColumnSelectionInterval(clickcolumn,clickcolumn);
                    }
                }
                else
                {
                    t.clearSelection();
                    t.addRowSelectionInterval(0,t.getRowCount()-1);
                    t.addColumnSelectionInterval(clickcolumn,clickcolumn);
                }
            }
        };
        t.getTableHeader().addMouseListener(listener);
    }
}
