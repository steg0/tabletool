package de.steg0.deskapps.tabletool;

import java.util.Map;

class JdbcBufferConfigSource
{
    private final PropertyHolder propertyHolder;
    private final ConnectionListModel connectionListModel;

    JdbcBufferConfigSource(PropertyHolder propertyHolder,
            ConnectionListModel connectionListModel)
    {
        this.propertyHolder = propertyHolder;
        this.connectionListModel = connectionListModel;
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
}
