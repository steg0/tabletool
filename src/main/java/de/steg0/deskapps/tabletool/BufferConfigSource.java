package de.steg0.deskapps.tabletool;

import java.awt.Color;
import java.io.File;
import java.util.Map;

import de.steg0.deskapps.tabletool.Connections.ConnectionState;

class BufferConfigSource
{
    final File pwd;
    int fetchsize;
    boolean updatableResultSets;

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

    Color getNonFocusedEditorBorderColor()
    {
        return propertyHolder.getNonFocusedEditorBorderColor();
    }

    Color getFocusedEditorBorderColor()
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

    Color getUnsavedEditorBorderColor()
    {
        return propertyHolder.getUnsavedEditorBorderColor();
    }
    Color getEditorBackgroundColor()
    {
        ConnectionState connectionState = connectionListModel.getSelectedItem();
        if(connectionState == null)
        {
            return propertyHolder.getDefaultBackground();
        }
        Color bg = connectionState.info().background;
        if(bg == null)
        {
            return propertyHolder.getDefaultBackground();
        }
        return bg;
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
