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
 * 
 * <p>
 * Initially I thought it'd be a nice practice using some of the native toolkits
 * around (WxWidgets, Qt &ndash; <em>not</em> freaking GTK &ndash;, &hellip;)
 * but then I realized that I want it to get to something real quick, and also
 * database connectivity is more flexible and more geared towards current
 * enterprise scenarios with Java. So Java is really the most obvious choice for
 * the tool.
 * </p>
 * 
 * <p>
 * Non-functional requirements I plan to meet:
 * </p>
 * 
 * <ul>
 * <li>Be light on memory and CPU usage. Prove to the world that this is
 * possible with Java. I think the tool should run with 32 MB of RAM on the
 * heap.</li>
 * <li>Try employing LambdaJ, since I think this is just the right thing to make
 * Java more elegant in certain places without having to wait for all these C#
 * rip-off JSRs.</li>
 * <li>Program in a pragmatic manner and toss all modeling ambitions.</li>
 * <li>Program in Swing alone. Don't use UI designers or 3rd party Swing
 * extensions. Don't use JavaFX (for now). Don't use the Swing Application
 * Framework.</li>
 * <li>Ship light. Don't ship JDBC or O/R libraries. If they should be supported
 * someday, load them dynamically from the target environment.</li>
 * <li>Don't force a specific Laf onto users. If a beautified Laf should be
 * devised for the application (I think of metal w/o the mauve), make it
 * optional. In fact, be one of these rare applications that can be 
 * used with the <tt>swing.defaultlaf</tt> property.</li>
 * <li>Derive as little as possible, associate as much as possible.</li>
 * </ul>
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
