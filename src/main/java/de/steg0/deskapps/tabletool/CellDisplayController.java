package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;

class CellDisplayController
{
    Logger logger = Logger.getLogger("tabtype");

    private final JFrame cellDisplay;
    private final File pwd;

    CellDisplayController(JFrame cellDisplay,JTable source,Consumer<String> log,
            File pwd,Font editorfont)
    {
        this.cellDisplay = cellDisplay;
        this.pwd = pwd;
        
        source.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                if(event.getClickCount() == 2)
                {
                    int row = source.rowAtPoint(event.getPoint()),
                        col = source.columnAtPoint(event.getPoint());        
                    showForSource(source,row,col,log,editorfont);
                }               
            }
        });
        source.addKeyListener(new KeyListener() {
            @Override public void keyPressed(KeyEvent event) {
                
                switch(event.getKeyCode())
                {
                case KeyEvent.VK_ENTER:
                    showForSource(source,source.getSelectedRow(),
                            source.getSelectedColumn(),log,editorfont);
                    event.consume();
                }
            }

            @Override public void keyTyped(KeyEvent e) { }
            @Override public void keyReleased(KeyEvent e) { }    
        });
    }

    void showForSource(JTable source,int row,int col,
            Consumer<String> log,Font editorfont)
    {
        if(row<0 || col<0)
        {
            return;
        }
        var rsm = (ResultSetTableModel)source.getModel();
        Object cellcontent = source.getValueAt(row,col);
        try
        {
            show(rsm.rs,rsm.updatable,cellcontent,col+1,editorfont);
        }
        catch(SQLException e)
        {
            log.accept(SQLExceptionPrinter.toString(e));
        }
        catch(IOException e)
        {
            StringBuilder b=new StringBuilder();
            b.append("IOException occured at ");
            b.append(new Date());
            b.append(":\n");
            b.append(e.getMessage());
            log.accept(b.toString());
        }
    }
    
    void show(ResultSet resultset,boolean updatable,Object value,int column,
            Font editorfont)
    throws SQLException,IOException
    {
        var textarea = new JTextArea(17,72);
        
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        String dialogtitle;

        logger.log(Level.INFO,"value is {0}",value.getClass());
        
        if(value instanceof Clob) try(var l = new LineNumberReader(
                ((Clob)value).getCharacterStream()))
        {
            var b = new StringBuilder();
            for(String line=l.readLine();line!=null;line=l.readLine())
            {
                b.append(line);
                b.append('\n');
            }
            textarea.setText(b.toString());
            textarea.setFont(editorfont);

            var updateButton = new JButton("Update & Close");
            var updateAction = new ClobUpdateAction();
            updateAction.textarea = textarea;
            updateAction.resultset = resultset;
            updateAction.column = column;
            updateButton.addActionListener(updateAction);
            updateButton.setEnabled(updatable);
            buttonPanel.add(updateButton);

            dialogtitle = "CLOB display - Tabtype";
        }
        else if(value instanceof Blob)
        {
            textarea.setEditable(false);
            var blob = (Blob)value;
            
            if(Desktop.isDesktopSupported())
            {
                var openButton = new JButton("Open");
                var openAction = new BlobOpenAction();
                openAction.blob = blob;
                openButton.addActionListener(openAction);
                buttonPanel.add(openButton);
            }
            
            var saveButton = new JButton("Export");
            var exportAction = new BlobExportAction();
            exportAction.blob = blob;
            saveButton.addActionListener(exportAction);
            buttonPanel.add(saveButton);
            
            var loadButton = new JButton("Import");
            var importAction = new BlobImportAction();
            importAction.blob = blob;
            importAction.resultset = resultset;
            importAction.column = column;
            loadButton.addActionListener(importAction);
            loadButton.setEnabled(updatable);
            buttonPanel.add(loadButton);

            try(var is = blob.getBinaryStream())
            {
                var dump = new HexDump(is,16*0x100);
                textarea.setFont(new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        textarea.getFont().getSize()));
                textarea.setText(dump.dump);
                dialogtitle = "BLOB bytes 0 to "+dump.length+" of "+
                    blob.length()+" - Tabtype";
            }
        }
        else if(value instanceof byte[] b)
        {
            textarea.setEditable(false);
            var dump = new HexDump(new ByteArrayInputStream(b),b.length);
            textarea.setFont(new Font(
                    Font.MONOSPACED,
                    Font.PLAIN,
                    textarea.getFont().getSize()));
            textarea.setText(dump.dump);
            dialogtitle = "byte["+b.length+"] display - Tabtype";
        }
        else
        {
            if(value!=null) textarea.setText(value.toString());
            textarea.setFont(editorfont);

            if(resultset!=null && updatable)
            {
                var updateButton = new JButton("Update & Close");
                updateButton.setMnemonic(KeyEvent.VK_U);
                var updateAction = new UpdateAction();
                updateAction.textarea = textarea;
                updateAction.resultset = resultset;
                updateAction.column = column;
                updateButton.addActionListener(updateAction);
                buttonPanel.add(updateButton);
            }

            dialogtitle = "Scalar value display - Tabtype";
        }
        textarea.setCaretPosition(0);

        cellDisplay.setTitle(dialogtitle);
        cellDisplay.getContentPane().removeAll();
        cellDisplay.setIconImages(Tabtype.getIcons());
        cellDisplay.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        cellDisplay.getContentPane().setLayout(new BorderLayout());
        
        var undoManager = new UndoManager();
        textarea.getDocument().addUndoableEditListener(undoManager);

        textarea.addKeyListener(new KeyListener()
        {
            @Override public void keyTyped(KeyEvent e) { }
            @Override public void keyPressed(KeyEvent e) { }

            @Override
            public void keyReleased(KeyEvent e)
            {
                switch(e.getKeyCode())
                {
                case KeyEvent.VK_ESCAPE:
                    cellDisplay.setVisible(false);
                    break;
                case KeyEvent.VK_Z:
                    if(e.isControlDown() && undoManager.canUndo())
                    {
                        undoManager.undo();
                    }
                    break;
                case KeyEvent.VK_Y:
                    if(e.isControlDown() && undoManager.canRedo())
                    {
                        undoManager.redo();
                    }
                }
            }
        });

        textarea.getDocument().addDocumentListener(
                new DocumentListener()
                {
                    @Override
                    public void insertUpdate(DocumentEvent e)
                    {
                        changedUpdate(e);
                    }
                    @Override
                    public void removeUpdate(DocumentEvent e)
                    {
                        changedUpdate(e);
                    }
                    @Override
                    public void changedUpdate(DocumentEvent e)
                    {
                        cellDisplay.setTitle("*"+dialogtitle);
                    }
                });
        
        var scrollpane = new JScrollPane(textarea);

        cellDisplay.getContentPane().add(scrollpane);
        
        var closeButton = new JButton("Close");
        closeButton.setMnemonic(KeyEvent.VK_C);
        closeButton.addActionListener((e) -> cellDisplay.setVisible(false));
        buttonPanel.add(closeButton);
        
        cellDisplay.getContentPane().add(buttonPanel,BorderLayout.SOUTH);
        
        if(!cellDisplay.isVisible()) cellDisplay.pack();
        cellDisplay.setVisible(true);
    }

    private static class HexDump
    {
        String dump;
        int length;
        
        private HexDump(InputStream is,int maxlength)
        throws IOException
        {
            var hex=new StringBuilder();
            var plain=new StringBuilder();
            int i=0;
            for(;i<maxlength;i++)
            {
                int b=is.read();
                if(b==-1)
                {
                    for(int j=0;j<16-i%16;j++) hex.append("   ");
                    hex.append(plain);
                    break;
                }
                if(i%16==0)
                {
                    if(i>0) hex.append(plain).append('\n');
                    plain.setLength(0);
                    hex.append(String.format("%04x ",i));
                }
                hex.append(String.format("%02x ",b));
                plain.append(b>=0&&b<32?'.':(char)b);
            }
            dump=hex.toString();
            length=i;
        }
    }
    
    private class BlobOpenAction implements ActionListener
    {
        Blob blob;

        @Override
        public void actionPerformed(ActionEvent event)
        {
            try(var is = new BufferedInputStream(blob.getBinaryStream()))
            {
                String suffix = SuffixGuess.fromStream(is);
                var tmpfile = File.createTempFile("ttblob",suffix);
                tmpfile.deleteOnExit();
                try(var os = new BufferedOutputStream(
                        new FileOutputStream(tmpfile)))
                {
                    byte[] buf=new byte[0x4000];
                    int len;
                    while((len=is.read(buf))!=-1) os.write(buf,0,len);
                }
                Desktop.getDesktop().open(tmpfile);
            }
            catch(SQLException e)
            {
                showExceptionDialog("Error opening",e);
            }
            catch(Exception e)
            {
                JOptionPane.showMessageDialog(
                        cellDisplay,
                        "Error opening: "+e.getMessage(),
                        "Error opening",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private class BlobExportAction implements ActionListener
    {
        Blob blob;
        
        @Override
        public void actionPerformed(ActionEvent event)
        {
            var filechooser = new JFileChooser(pwd);
            int returnVal = filechooser.showSaveDialog(cellDisplay);
            if(returnVal != JFileChooser.APPROVE_OPTION) return;
            File file=filechooser.getSelectedFile();
            try(InputStream is = blob.getBinaryStream();
                var os = new BufferedOutputStream(new FileOutputStream(file)))
            {
                byte[] buf=new byte[0x4000];
                int len;
                while((len=is.read(buf))!=-1) os.write(buf,0,len);
            }
            catch(SQLException e)
            {
                showExceptionDialog("Error exporting",e);
            }
            catch(IOException e)
            {
                JOptionPane.showMessageDialog(
                        cellDisplay,
                        "Error exporting: "+e.getMessage(),
                        "Error exporting",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private class BlobImportAction implements ActionListener
    {
        Blob blob;
        ResultSet resultset;
        int column;
        
        @Override
        public void actionPerformed(ActionEvent event)
        {
            var filechooser = new JFileChooser(pwd);
            int returnVal = filechooser.showOpenDialog(cellDisplay);
            if(returnVal != JFileChooser.APPROVE_OPTION) return;
            File file=filechooser.getSelectedFile();
            
            try
            {
                blob.truncate(0);
                
                try(var is = new BufferedInputStream(new FileInputStream(file));
                        OutputStream os = blob.setBinaryStream(1))
                {
                    byte[] buf=new byte[0x4000];
                    int len;
                    while((len=is.read(buf))!=-1) os.write(buf,0,len);
                }
                
                resultset.updateBlob(column,blob);
                resultset.updateRow();
            }
            catch(SQLException e)
            {
                showExceptionDialog("Error importing",e);
            }
            catch(Exception e)
            {
                JOptionPane.showMessageDialog(
                        cellDisplay,
                        "Error importing: "+e.getMessage(),
                        "Error importing",
                        JOptionPane.ERROR_MESSAGE);
            }
            cellDisplay.setVisible(false);
        }
    }
    
    private class UpdateAction implements ActionListener
    {
        JTextArea textarea;
        ResultSet resultset;
        int column;
        
        @Override
        public void actionPerformed(ActionEvent event)
        {
            try
            {
                resultset.updateString(column,textarea.getText());
                resultset.updateRow();
            }
            catch(SQLException e)
            {
                showExceptionDialog("Error updating",e);
            }
            cellDisplay.setVisible(false);
        }
    }
    
    private class ClobUpdateAction implements ActionListener
    {
        JTextArea textarea;
        ResultSet resultset;
        int column;
        
        @Override
        public void actionPerformed(ActionEvent event)
        {
            try
            {
                resultset.updateClob(column,new StringReader(
                        textarea.getText()));
                resultset.updateRow();
            }
            catch(SQLException e)
            {
                showExceptionDialog("Error updating",e);
            }
            cellDisplay.setVisible(false);
        }
    }

    private void showExceptionDialog(String title,SQLException exc)
    {
        String errorText = SQLExceptionPrinter.toString(exc);
        var dialog = new JDialog(cellDisplay,title,true);
        var label = new JLabel(title + ":");
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(label,BorderLayout.NORTH);
        var errorPane = new JTextArea(errorText);
        errorPane.setEditable(false);
        var scrollPane = new JScrollPane(errorPane);
        dialog.getContentPane().add(scrollPane,BorderLayout.CENTER);
        var button = new JButton("OK");
        button.addActionListener(e -> dialog.dispose());
        var buttonPanel = new JPanel();
        buttonPanel.add(button);        
        dialog.getContentPane().add(buttonPanel,BorderLayout.SOUTH);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setLocationRelativeTo(cellDisplay);
        button.requestFocusInWindow();
        dialog.getRootPane().setDefaultButton(button);
        dialog.setVisible(true);
    }
}
