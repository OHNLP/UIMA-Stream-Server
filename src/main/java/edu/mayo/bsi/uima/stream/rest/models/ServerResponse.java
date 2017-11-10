package edu.mayo.bsi.uima.stream.rest.models;


import java.io.Serializable;
import java.util.Map;

/**
 * A Server Response to a NLP Request, interactions with this class should only be done through
 * {@link edu.mayo.bsi.uima.stream.rest.UIMARESTServer}
 */
public class ServerResponse {
    private final long jobDuration;
    private final String metadata;
    private final String message;
    private final Map<String, Serializable> content;

    public ServerResponse(long jobDuration, String metadata, String message, Map<String, Serializable> content) {
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

    public Map<String, Serializable> getContent() {
        return content;
    }
}
