package de.steg0.deskapps.tabletool;

import java.awt.Color;
import java.io.File;
import java.text.ParseException;
import java.util.Map;

import de.steg0.deskapps.tabletool.Connections.ConnectionState;

class BufferConfigSource
{
    final File pwd;
    int fetchsize;
    boolean updatableResultSets;
    boolean autocommit;

    private final PropertyHolder propertyHolder;
    private final ConnectionListModel connectionListModel;

    BufferConfigSource(PropertyHolder propertyHolder,
            ConnectionListModel connectionListModel,File pwd)
    {
        this.propertyHolder = propertyHolder;
        this.connectionListModel = connectionListModel;
        this.pwd = pwd;
    }

    int getResultViewHeight()
    {
        return propertyHolder.getResultviewHeight();
    }

    String getEditorFontName()
    {
        return propertyHolder.getEditorFontName();
    }

    Integer getEditorFontSize()
    {
        return propertyHolder.getEditorFontSize();        
    }

    int getEditorTabsize()
    {
        return propertyHolder.getEditorTabsize();
    }

    Color getNonFocusedEditorBorderColor() throws ParseException
    {
        return propertyHolder.getNonFocusedEditorBorderColor();
    }

    Color getFocusedEditorBorderColor() throws ParseException
    {
        ConnectionState connectionState = connectionListModel.getSelectedItem();
        if(connectionState == null)
        {
            return propertyHolder.getFocusedEditorBorderColor();
        }
        Color focusedBorderColor = connectionState.info().focusedBorderColor;
        if(focusedBorderColor == null)
        {
            return propertyHolder.getFocusedEditorBorderColor();
        }
        return focusedBorderColor;
    }

    Color getUnsavedEditorBorderColor() throws ParseException
    {
        return propertyHolder.getUnsavedEditorBorderColor();
    }
    /**Returns background color to use for unconnected editors. */
    Color getDefaultBackgroundColor(Color fallback) throws ParseException
    {
        return propertyHolder.getDefaultBackground(fallback);
    }
    /**Returns background color, taking connection state into account. */
    Color getEditorBackgroundColor(Color fallback) throws ParseException
    {
        ConnectionState connectionState = connectionListModel.getSelectedItem();
        if(connectionState == null)
        {
            return propertyHolder.getDefaultBackground(fallback);
        }
        Color bg = connectionState.info().background;
        if(bg == null)
        {
            bg = propertyHolder.getDefaultConnectedBackground();
        }
        if(bg == null)
        {
            bg = propertyHolder.getDefaultBackground(fallback);
        }
        return bg;
    }
    boolean colorizeJavaLafTables()
    {
        return propertyHolder.colorizeJavaLafTables();
    }

    String getCompletionTemplate()
    {
        ConnectionState connectionState = connectionListModel.getSelectedItem();
        return connectionState.info().completionTemplate;
    }

    String getInfoTemplate()
    {
        ConnectionState connectionState = connectionListModel.getSelectedItem();
        return connectionState.info().infoTemplate;
    }

    Map<String,String> getSnippetTemplates()
    {
        ConnectionState connectionState = connectionListModel.getSelectedItem();
        return connectionState.info().snippetTemplates;
    }

    String getPlaceholderRegex()
    {
        return propertyHolder.getPlaceholderRegex();
    }
}
