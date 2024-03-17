package de.steg0.deskapps.tabletool;

import java.io.File;
import java.util.Map;

class BufferConfigSource
{
    private final PropertyHolder propertyHolder;
    private final ConnectionListModel connectionListModel;
    final File pwd;

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
    int getEditorTabsize()
    {
        return propertyHolder.getEditorTabsize();
    }
    String getCompletionTemplate()
    {
        Connections.ConnectionState connectionState = 
            (Connections.ConnectionState)connectionListModel.getSelectedItem();
        return connectionState.info().completionTemplate;
    }
    String getInfoTemplate()
    {
        Connections.ConnectionState connectionState = 
            (Connections.ConnectionState)connectionListModel.getSelectedItem();
        return connectionState.info().infoTemplate;
    }
    Map<String,String> getSnippetTemplates()
    {
        Connections.ConnectionState connectionState = 
            (Connections.ConnectionState)connectionListModel.getSelectedItem();
        return connectionState.info().snippetTemplates;
    }
    String getPlaceholderRegex()
    {
        return propertyHolder.getPlaceholderRegex();
    }
}
