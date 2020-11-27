package de.steg0.deskapps.tabletool;

public class Workspace
{
    private String[] files;

    public String[] getFiles()
    {
        return files;
    }

    public void setFiles(String[] files)
    {
        this.files = files;
    }
    
    private String[] recentFiles;

    public String[] getRecentFiles()
    {
        return recentFiles;
    }

    public void setRecentFiles(String[] recentFiles)
    {
        this.recentFiles = recentFiles;
    }
}
