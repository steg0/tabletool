package de.steg0.deskapps.tabletool;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;

class NotebookLogConsumer implements Consumer<String>,ActionListener
{
    Logger resultlog = Logger.getLogger("tabtype");

    private Border 
        regularBorder = BorderFactory.createDashedBorder(Color.WHITE),
        hilightedBorder = BorderFactory.createDashedBorder(Color.BLUE);

    private final JTextArea log;
    private Timer unhilighter;

    NotebookLogConsumer(JTextArea log)
    {
        this.log = log;
        log.setBorder(regularBorder);
    }

    public synchronized void accept(String t)
    {
        resultlog.fine(t);
        SwingUtilities.invokeLater(() ->
        {
            log.setBorder(hilightedBorder);
            log.setText(t);
            if(unhilighter!=null) unhilighter.stop();
            unhilighter = new Timer(1500,this);
            unhilighter.setRepeats(false);
            unhilighter.start();
        });
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        log.setBorder(regularBorder);
    }
}