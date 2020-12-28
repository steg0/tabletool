package de.steg0.deskapps.tabletool;

import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * A damage event to be sent to a {@link JTextArea} to recalculate its
 * longest line, by listing all elements for add and remove.
 * <p>
 * The motivation behind this is the following observed behavior when
 * testing Myriad with JDK 13 on nt2:
 * <ol>
 * <li>
 * Type the following text in the text area:
 * <pre>
 * abcd
 * abcdef
 * abcdef
 * </pre>
 * <li>
 * Select like this:
 * <pre>
 * abcd
 * a[bcdef
 * abcde]f
 * </pre>
 * <li>
 * Press DEL
 * </ol>
 * This caused the longest line to be calculated as line 1, when it
 * was in fact now line 0, resulting in a too narrow view for the text. 
 */
class ExtendTextDamageEvent implements DocumentEvent
{
    static void send(JTextArea editor,DocumentEvent miniDamageEvent)
    {
        var event = new ExtendTextDamageEvent();
        event.miniDamageEvent = miniDamageEvent;
        editor.getUI().getRootView(editor).changedUpdate(event,null,null);
    }
    
    DocumentEvent miniDamageEvent;

    @Override
    public int getOffset()
    {
        return 0;
    }
    
    @Override
    public int getLength()
    {
        return miniDamageEvent.getDocument().getLength();
    }
    
    @Override
    public Document getDocument()
    {
        return miniDamageEvent.getDocument();
    }
    
    @Override
    public EventType getType()
    {
        return miniDamageEvent.getType();
    }
    
    @Override
    public ElementChange getChange(Element elem)
    {
        var ec = new ExtendDamageElementChange();
        ec.document = miniDamageEvent.getDocument();
        return ec;
    }
}

class ExtendDamageElementChange implements ElementChange
{
    Document document;

    @Override
    public Element getElement()
    {
        return document.getDefaultRootElement();
    }

    @Override
    public int getIndex()
    {
        return 0;
    }

    @Override
    public Element[] getChildrenRemoved()
    {
        return document.getRootElements();
    }

    @Override
    public Element[] getChildrenAdded()
    {
        return document.getRootElements();
    }
}
