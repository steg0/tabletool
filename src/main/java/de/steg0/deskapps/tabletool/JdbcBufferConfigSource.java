package de.steg0.deskapps.tabletool;

import java.util.Map;

class JdbcBufferConfigSource
{
    PropertyHolder propertyHolder;
    ConnectionListModel connectionListModel;

    JdbcBufferConfigSource(PropertyHolder propertyHolder,
            ConnectionListModel connectionListModel)
    {
        this.propertyHolder = propertyHolder;
        this.connectionListModel = connectionListModel;
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
