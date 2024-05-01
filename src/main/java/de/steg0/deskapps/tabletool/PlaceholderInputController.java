package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;

class PlaceholderInputController
{
    private BufferConfigSource configSource;
    private JFrame parent;
    private String[][] lastValues;

    PlaceholderInputController(BufferConfigSource configSource,
            JFrame parent)
    {
        this.configSource=configSource;
        this.parent = parent;
    }

    /**
     * Displays a dialog for the user to supply values for all detected
     * placeholders and returns the modified string. If none are detected,
     * returns the original string. The user can also edit the placeholders
     * in the displayed table. This is probably not often useful, but
     * there is no reason not to allow it.
     */
    String fill(String s)
    {
        String[] placeholderOccurrences = new PlaceholderSupport(configSource)
                .getPlaceholderOccurrences(s);
        if(placeholderOccurrences.length>0)
        {
            String[][] placeholderMap =
                    new String[placeholderOccurrences.length][];
            for(int i=0;i<placeholderMap.length;i++)
            {
                String newValue = null;
                /* try pre-fill with last remembered values */
                if(lastValues!=null) for(String[] kv : lastValues)
                {
                    if(kv[0].equals(placeholderOccurrences[i]))
                    {
                        newValue = kv[1];
                        break;
                    }
                }
                placeholderMap[i] =
                        new String[]{placeholderOccurrences[i],newValue};
            }
            var f = new JDialog(parent,"Placeholder Input",true);
            f.setLocationRelativeTo(parent);
            f.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            ((BorderLayout)f.getContentPane().getLayout()).setVgap(5);
            var explanation = new JLabel("Please provide values "
                    +" for placeholders found in the query:");
            f.getContentPane().add(explanation,BorderLayout.NORTH);
            JTable table = new JTable(placeholderMap,
                    new String[]{"Placeholder","Replacement"});
            f.getContentPane().add(table);
            var closeButton = new JButton("Close");
            closeButton.addActionListener(e -> f.dispose());
            f.getContentPane().add(closeButton,BorderLayout.SOUTH);
            f.getRootPane().setDefaultButton(closeButton);
            f.pack();

            f.setVisible(true);

            var placeholderSupport = new PlaceholderSupport(configSource);
            for(String[] replacement : placeholderMap)
            {
                if(replacement[1]!=null)
                {
                    s = placeholderSupport.quotedReplaceInString(s,
                            replacement[0],replacement[1]);
                }
            }
            lastValues = placeholderMap;
        }
        return s;
    }
}