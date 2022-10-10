package de.steg0.deskapps.tabletool;

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
}
