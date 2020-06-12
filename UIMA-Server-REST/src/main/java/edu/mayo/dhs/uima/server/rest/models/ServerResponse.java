package edu.mayo.dhs.uima.server.rest.models;


import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.dhs.uima.server.rest.UIMARESTServer;

import java.io.Serializable;
import java.util.Map;

/**
 * A Server Response to a NLP Request, interactions with this class should only be done through
 * {@link UIMARESTServer}
 */
public class ServerResponse {
    private long jobDuration = 0;
    private String metadata = null;
    private String message = null;
    private Map<String, JsonNode> content = null;

    public ServerResponse() {}

    public ServerResponse(long jobDuration, String metadata, String message, Map<String, JsonNode> content) {
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

    public Map<String, JsonNode> getContent() {
        return content;
    }

    public void setJobDuration(long jobDuration) {
        this.jobDuration = jobDuration;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setContent(Map<String, JsonNode> content) {
        this.content = content;
    }
}
