package de.steg0.deskapps.tabletool;

class NotebookSearchState
{
    String text;
    int initialTab;
    int buf;
    int loc;
    void set(int buf,int loc)
    {
        this.buf=buf;
        this.loc=loc;
    }
    void resetToPoint(TabSetController tabsetController,boolean forward)
    {
        NotebookController currentNotebook = tabsetController.getSelected();
        BufferController currentBuffer = currentNotebook.lastFocused();
        int currentBufferIndex = currentNotebook.buffers.indexOf(
                currentBuffer);
        this.initialTab=tabsetController.tabbedPane.getSelectedIndex();
        this.buf=currentBufferIndex;
        if(forward) this.loc=currentBuffer.editor.getSelectionEnd();
        else this.loc=currentBuffer.editor.getSelectionStart();
    }
}