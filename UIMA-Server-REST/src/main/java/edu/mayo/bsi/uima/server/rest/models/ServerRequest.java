package edu.mayo.bsi.uima.server.rest.models;

import java.util.Collection;

public class ServerRequest {
    private final String streamName;
    private final String metadata;
    private final String document;
    private final Collection<String> serializers;

    public ServerRequest(String streamName, String metadata, String document, Collection<String> serializers) {
        this.streamName = streamName;
        this.metadata = metadata;
        this.document = document;
        this.serializers = serializers;
    }

    public String getStreamName() {
        return streamName;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getDocument() {
        return document;
    }

    public Collection<String> getSerializers() {
        return serializers;
    }
}
