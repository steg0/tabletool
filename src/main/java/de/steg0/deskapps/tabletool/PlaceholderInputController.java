package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellEditor;

class PlaceholderInputController
{
    private static final Pattern TEXTPATTERN = Pattern.compile(
            "^(" +
            "\\'[^\\']*\\'" +
            "|" +
            "\\-[^\\-]*" +
            "|" +
            "\\/[^\\*\\/\\']*" +
            "|" +
            "[^\\-\\/\\']*" +
            ")");
    private static final Pattern COMMENTPATTERN = Pattern.compile(
            "^(" +
            "\\-\\-.*?\\n" +
            "|" +
            "\\/\\*.*?\\*\\/" +
            ")*",Pattern.DOTALL);
   
    private BufferConfigSource configSource;
    private JFrame parent;
    private String[][] lastValues;

    static class SubstitutionCanceledException extends Exception
    {
    }

    static String stripComments(String s)
    {
        var stripped=new StringBuilder();
        var remainder = s;
        while(!remainder.isEmpty())
        {
            var textMatcher = TEXTPATTERN.matcher(remainder);
            if(textMatcher.find()) 
            {
                String match = textMatcher.group(1);
                stripped.append(match);
                remainder = remainder.substring(match.length());
            }
            else assert false;
            var commentMatcher = COMMENTPATTERN.matcher(remainder);
            remainder = commentMatcher.replaceAll("");
        }
        return stripped.toString();
    }

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
    String fill(String s) throws SubstitutionCanceledException
    {
        String[] placeholderOccurrences = new PlaceholderSupport(configSource)
                .getPlaceholderOccurrences(stripComments(s));
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

            var explanation = new JTextArea("Please provide values " +
                    "for placeholders found in the query.\n" +
                    "Use Enter to accept a value in a cell, and Ctrl+Enter " +
                    "to proceed.");
            explanation.setEditable(false);
            f.getContentPane().add(explanation,BorderLayout.NORTH);
            
            JTable table = new JTable(placeholderMap,
                    new String[]{"Placeholder","Replacement"});
            f.getContentPane().add(table);

            var closeButton = new JButton("Close and Proceed");
            boolean[] proceed={false};
            closeButton.addActionListener(e ->
            {
                proceed[0] = true;
                f.dispose();
            });
            f.getContentPane().add(closeButton,BorderLayout.SOUTH);

            f.getRootPane().setDefaultButton(closeButton);
            f.getRootPane().registerKeyboardAction(
                    evt -> f.dispose(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
            f.pack();

            table.setRowSelectionInterval(0,0);
            table.setColumnSelectionInterval(1,1);
            table.editCellAt(0,1);
            table.requestFocusInWindow();

            f.setVisible(true);

            if(!proceed[0]) throw new SubstitutionCanceledException();

            TableCellEditor editor = table.getCellEditor();
            if(editor!=null) editor.stopCellEditing();

            var placeholderSupport = new PlaceholderSupport(configSource);
            for(String[] replacement : placeholderMap)
            {
                if(replacement[1]!=null)
                {
                    s = placeholderSupport.quotedReplaceInString(s,
                            replacement[0],replacement[1]);
                }
                else
                {
                    s = placeholderSupport.quotedReplaceInString(s,
                            replacement[0],"");
                }
            }
            lastValues = placeholderMap;
        }
        return s;
    }
}