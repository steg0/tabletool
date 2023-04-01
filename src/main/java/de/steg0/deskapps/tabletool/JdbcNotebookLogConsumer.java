package de.steg0.deskapps.tabletool;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.border.Border;

class JdbcNotebookLogConsumer implements Consumer<String>,ActionListener
{
    private Border 
        regularBorder = BorderFactory.createDashedBorder(Color.WHITE),
        hilightedBorder = BorderFactory.createDashedBorder(Color.BLUE);

    private final JTextArea log;
    private Timer unhilighter;

    JdbcNotebookLogConsumer(JTextArea log)
    {
        this.log = log;
        log.setBorder(regularBorder);
    }

    public void accept(String t)
    {
        log.setBorder(hilightedBorder);
        log.setText(t);
        if(unhilighter!=null) unhilighter.stop();
        unhilighter = new Timer(1500,this);
        unhilighter.setRepeats(false);
        unhilighter.start();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        log.setBorder(regularBorder);
    }
}