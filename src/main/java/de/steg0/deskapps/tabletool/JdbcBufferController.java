package de.steg0.deskapps.tabletool;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Date;
import java.util.function.Consumer;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;

class JdbcBufferController
implements KeyListener
{
    static final MessageFormat UPDATE_LOG_FORMAT = 
            new MessageFormat("{0} row{0,choice,0#s|1#|1<s} updated\n");

    JPanel panel = new JPanel(new GridBagLayout());
    Connection connection;
    JTextArea editor = new JTextArea();
    Consumer<String> updateLog;
    
    JdbcBufferController(Consumer<String> updateLog)
    {
        this.updateLog = updateLog;
        
        var editorConstraints = new GridBagConstraints();
        editorConstraints.anchor = GridBagConstraints.WEST;
        panel.add(editor,editorConstraints);
        
        editor.addKeyListener(this);
    }

    @Override
    public void keyReleased(KeyEvent event)
    {
        switch(event.getKeyCode())
        {
        case KeyEvent.VK_ENTER:
            if(event.isControlDown()) fetch();
        }
    }
    
    @Override public void keyTyped(KeyEvent e) { }
    @Override public void keyPressed(KeyEvent e) { }
    
    void focusEditor()
    {
        editor.requestFocusInWindow();
    }

    /**blocking */
    ResultSet fetch()
    {
        String text = editor.getText();
        try(Statement st = connection.createStatement())
        {
            if(st.execute(text))
            {
                try(ResultSet rs = st.getResultSet())
                {
                    ResultSetTableModel rsm = new ResultSetTableModel();
                    rsm.update(rs);
                    JTable resultview = new JTable(rsm);
                    if(panel.getComponentCount()==2) panel.remove(1);
                    var resultviewConstraints = new GridBagConstraints();
                    resultviewConstraints.anchor = GridBagConstraints.WEST;
                    resultviewConstraints.gridy = 1;
                    panel.add(resultview,resultviewConstraints);
                    panel.revalidate();
                    updateLog.accept("");
                }
            }
            else
            {
                updateLog.accept(UPDATE_LOG_FORMAT.format(st.getUpdateCount()));
            }
        }
        catch(SQLException e)
        {
            StringBuilder b=new StringBuilder();
            b.append("Error executing SQL at ");
            b.append(new Date());
            b.append("\n");
            for(;e!=null;e=e.getNextException())
            {
                b.append("Error code: "+e.getErrorCode()+"\n");
                b.append("SQL State: "+e.getSQLState()+"\n");
                b.append(e.getMessage()+"\n");
            }
            updateLog.accept(b.toString());
        }
        catch(NullPointerException e)
        {
            StringBuilder b=new StringBuilder();
            b.append("No connection available at ");
            b.append(new Date());
            b.append("\n");
            updateLog.accept(b.toString());
        }
        return null;
    }
}
