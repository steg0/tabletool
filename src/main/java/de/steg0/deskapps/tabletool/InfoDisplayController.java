package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

class InfoDisplayController
{
    final JFrame infoDisplay;
    final JTable content;
    final Consumer<String> log;

    JPanel panel = new JPanel(new BorderLayout());
    
    InfoDisplayController(JFrame infoDisplay,JTable content,Consumer<String> log)
    {
        this.infoDisplay = infoDisplay;
        this.content = content;
        this.log = log;
        show();
    }

    void show()
    {
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        String dialogtitle = "Info display";
        
        infoDisplay.setTitle(dialogtitle);
        infoDisplay.getContentPane().removeAll();
        infoDisplay.setIconImages(Tabtype.getIcons());
        infoDisplay.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        infoDisplay.getContentPane().setLayout(new BorderLayout());
        
        content.addKeyListener(new KeyListener()
        {
            @Override public void keyTyped(KeyEvent e) { }
            @Override public void keyPressed(KeyEvent e) { }

            @Override
            public void keyReleased(KeyEvent e)
            {
                switch(e.getKeyCode())
                {
                case KeyEvent.VK_ESCAPE:
                    infoDisplay.setVisible(false);
                }
            }
        });
        
        var scrollpane = new JScrollPane(content);

        infoDisplay.getContentPane().add(scrollpane);
        
        var closeButton = new JButton("Close");
        closeButton.setMnemonic(KeyEvent.VK_C);
        closeButton.addActionListener((e) -> infoDisplay.setVisible(false));
        buttonPanel.add(closeButton);
        
        infoDisplay.getContentPane().add(buttonPanel,BorderLayout.SOUTH);
        
        if(!infoDisplay.isVisible()) infoDisplay.pack();
        infoDisplay.setVisible(true);
    }
}
