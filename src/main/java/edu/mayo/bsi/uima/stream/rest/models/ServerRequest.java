package edu.mayo.bsi.uima.stream.rest.models;

public class ServerRequest {
    private final String streamName;
    private final String metadata;
    private final String document;

    public ServerRequest(String streamName, String metadata, String document) {
        this.streamName = streamName;
        this.metadata = metadata;
        this.document = document;
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
}
