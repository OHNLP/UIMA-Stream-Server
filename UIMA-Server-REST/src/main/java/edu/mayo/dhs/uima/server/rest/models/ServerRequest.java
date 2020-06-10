package edu.mayo.dhs.uima.server.rest.models;

import java.util.Collection;

public class ServerRequest {
    private String streamName = null;
    private String metadata = null;
    private String document = null;
    private Collection<String> serializers = null;

    public ServerRequest() {}

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

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public void setSerializers(Collection<String> serializers) {
        this.serializers = serializers;
    }
}
