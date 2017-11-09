package edu.mayo.bsi.uima.stream.rest.models;


import java.io.Serializable;

/**
 * A Server Response to a NLP Request, interactions with this class should only be done through
 * {@link edu.mayo.bsi.uima.stream.rest.UIMARESTServer}
 */
public class ServerResponse {
    private final long jobDuration;
    private final String metadata;
    private final String message;
    private final Serializable content;

    public ServerResponse(long jobDuration, String metadata, String message, Serializable content) {
        this.jobDuration = jobDuration;
        this.metadata = metadata;
        this.message = message;
        this.content = content;
    }

    public long getJobDuration() {
        return jobDuration;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getMessage() {
        return message;
    }

    public Serializable getContent() {
        return content;
    }
}
