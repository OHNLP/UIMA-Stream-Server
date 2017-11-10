package edu.mayo.bsi.uima.stream.rest;

import edu.mayo.bsi.uima.stream.api.UIMANLPResultSerializer;
import edu.mayo.bsi.uima.stream.api.UIMAStream;
import edu.mayo.bsi.uima.stream.core.UIMAServerBase;
import edu.mayo.bsi.uima.stream.rest.models.ServerRequest;
import edu.mayo.bsi.uima.stream.rest.models.ServerResponse;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@SpringBootApplication
public class UIMARESTServer extends UIMAServerBase {

    private Map<String, UIMAStream> streams;
    private MultiValueMap<String, UIMANLPResultSerializer> resultSerializers;
    @Override
    public void start() {

    }

    @RequestMapping(method = RequestMethod.POST)
    CompletableFuture<ServerResponse> submitJob(@RequestBody ServerRequest req) {
        CompletableFuture<ServerResponse> ret = new CompletableFuture<>();
        UIMAStream stream = streams.get(req.getStreamName().toLowerCase());

        if (stream == null) {
            ret.completeExceptionally(new IllegalArgumentException("There is no currently running stream called " + req.getStreamName()));
            return ret;
        }
        CompletableFuture<CAS> pipelineResult = stream.submit(req.getDocument(), req.getMetadata());

        return ret;
    }

    private UIMAStream getStream(String streamName) {
        return streams.get(streamName.toLowerCase());
    }

    private void registerStream(String streamName, AnalysisEngineDescription metadataDesc, AnalysisEngineDescription pipelineDesc) {
        if (getStream(streamName) != null) {
            throw new IllegalStateException("A stream with " + streamName + " has already been registered!");
        }
    }
}
