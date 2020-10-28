package de.steg0.deskapps.tabletool;

import java.awt.BorderLayout;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Date;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

class CellDisplayController
{
    JPanel panel = new JPanel(new BorderLayout());
    JFrame parent;
    
    CellDisplayController(JFrame parent,JTable source,Consumer<String> log)
    {
        this.parent = parent;
        
        source.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                if(event.getClickCount() == 2)
                {
                    int row = source.rowAtPoint(event.getPoint()),
                        col = source.columnAtPoint(event.getPoint());
                    Object cellcontent = source.getValueAt(row,col);
                    try
                    {
                        show(cellcontent);
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
            }
        });
    }
    
    /**blocking*/
    void show(Object value)
    throws SQLException,IOException
    {
        var textarea = new JTextArea(10,72);
        textarea.setEditable(false);
        
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        String dialogtitle;
        
        if(value instanceof Clob)
        {
            var b = new StringBuilder();
            var l = new LineNumberReader(((Clob)value).getCharacterStream());
            for(String line=l.readLine();line!=null;line=l.readLine())
            {
                b.append(line);
            }
            textarea.setText(b.toString());
            dialogtitle = "CLOB display";
        }
        else if(value instanceof Blob)
        {
            var blob = (Blob)value;
            
            var saveButton = new JButton("Export");
            var exportAction = new BlobExportAction();
            exportAction.blob = blob;
            saveButton.addActionListener(exportAction);
            buttonPanel.add(saveButton);
            
            var loadButton = new JButton("Import");
            var importAction = new BlobImportAction();
            importAction.blob = blob;
            loadButton.addActionListener(importAction);
            buttonPanel.add(loadButton);

            var dump = new HexDump(blob,16*0x100);
            textarea.setFont(new Font(
                    Font.MONOSPACED,
                    Font.PLAIN,
                    textarea.getFont().getSize()));
            textarea.setText(dump.dump);
            dialogtitle = "BLOB bytes 0 to "+dump.length+" of "+blob.length();
        }
        else
        {
            if(value!=null) textarea.setText(value.toString());
            dialogtitle = "Scalar value display";
        }
        
        var dialog = new JDialog(parent,dialogtitle,true);

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setLayout(new BorderLayout());
        
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
                    dialog.dispose();
                }
            }
        });
        
        var scrollpane = new JScrollPane(textarea);

        dialog.getContentPane().add(scrollpane);
        
        var closeButton = new JButton("Close");
        closeButton.addActionListener((e) -> dialog.dispose());
        buttonPanel.add(closeButton);
        
        dialog.getContentPane().add(buttonPanel,BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(parent.getContentPane());
        dialog.setVisible(true);
    }

    static class HexDump
    {
        String dump;
        int length;
        
        HexDump(Blob blob,int maxlength)
        throws SQLException,IOException
        {
            try(InputStream is = blob.getBinaryStream())
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
    }
    
    class BlobExportAction implements ActionListener
    {
        Blob blob;
        
        /**blocking */
        @Override
        public void actionPerformed(ActionEvent event)
        {
            var filechooser = new JFileChooser();
            int returnVal = filechooser.showSaveDialog(parent);
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
                JOptionPane.showMessageDialog(
                        parent,
                        "Error exporting: "+SQLExceptionPrinter.toString(e),
                        "Error exporting",
                        JOptionPane.ERROR_MESSAGE);
            }
            catch(IOException e)
            {
                JOptionPane.showMessageDialog(
                        parent,
                        "Error exporting: "+e.getMessage(),
                        "Error exporting",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    class BlobImportAction implements ActionListener
    {
        Blob blob;
        
        /**blocking */
        @Override
        public void actionPerformed(ActionEvent event)
        {
            var filechooser = new JFileChooser();
            int returnVal = filechooser.showOpenDialog(parent);
            if(returnVal != JFileChooser.APPROVE_OPTION) return;
            File file=filechooser.getSelectedFile();
            
            try(var is = new BufferedInputStream(new FileInputStream(file));
                OutputStream os = blob.setBinaryStream(0))
            {
                byte[] buf=new byte[0x4000];
                int len;
                while((len=is.read(buf))!=-1) os.write(buf,0,len);
            }
            catch(SQLException e)
            {
                JOptionPane.showMessageDialog(
                        parent,
                        "Error importing: "+SQLExceptionPrinter.toString(e),
                        "Error importing",
                        JOptionPane.ERROR_MESSAGE);
            }
            catch(IOException e)
            {
                JOptionPane.showMessageDialog(
                        parent,
                        "Error importing: "+e.getMessage(),
                        "Error importing",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
}
