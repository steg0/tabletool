package de.steg0.deskapps.tabletool;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

class Workspaces
{
    static Workspace load(File f)
    throws IOException
    {
        try(var d=new XMLDecoder(new BufferedInputStream(
                new FileInputStream(f))))
        {
            return (Workspace)d.readObject();
        }
    }

    static void store(Workspace workspace,File f)
    throws IOException
    {
        try(var e=new XMLEncoder(new BufferedOutputStream(
                new FileOutputStream(f))))
        {
            e.writeObject(workspace);
        }
    }
}
