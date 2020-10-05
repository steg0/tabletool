package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;

import javax.swing.JFrame;

/**
 * The steg0 Tabletool.
 * 
 * <p>
 * This aims to be a simple tabular grid that accepts LISP expressions in its
 * cells. I. e. like Excel but without Basic.
 * </p>
 * 
 * <p>It should also offer table-diff functionality.</p>
 * 
 * <p>
 * I plan to add database connectivity later on, if I have a need (which I think
 * I'll have) and the necessary spare time.
 * </p>
 */
public class TableTool 
{

    private JFrame frame;
    
    public void showFrame()
    {
        this.frame = new JFrame("Tabletool");
        this.frame.getContentPane().setLayout(new BorderLayout());
        
        final WbPanel wbPanel = new WbPanel();

        /* 
         * Maybe introduce tabbed wb's someday that can be dragged between
         * frames, like Firefox
         */
        this.frame.getContentPane().add(wbPanel.getPanel(),BorderLayout.CENTER);
        
        this.frame.pack();
        this.frame.setVisible(true);
    }
    
    public static void main(String[] args)
    {
        
        final TableTool ttool = new TableTool();
        ttool.showFrame();
        
    }
    
}
