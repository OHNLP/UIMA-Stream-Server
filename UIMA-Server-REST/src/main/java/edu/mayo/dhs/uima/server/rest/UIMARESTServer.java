package edu.mayo.dhs.uima.server.rest;

import edu.mayo.dhs.uima.server.api.UIMANLPResultSerializer;
import edu.mayo.dhs.uima.server.api.UIMAStream;
import edu.mayo.dhs.uima.server.core.UIMAServerBase;
import edu.mayo.dhs.uima.server.rest.models.ServerRequest;
import edu.mayo.dhs.uima.server.rest.models.ServerResponse;
import org.apache.uima.cas.CAS;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A runnable Spring Boot application that handles POST requests via REST and submits asynchronously to
 * a UIMA pipeline and returns the result through serializers specified in the original request
 */
@RestController
@SpringBootApplication
public class UIMARESTServer extends UIMAServerBase {

    private Logger logger;

    @Override
    public void start() {
        logger = Logger.getLogger("UIMA-REST-Server");
        // No special initialization tasks needed
    }

    @RequestMapping(method = RequestMethod.POST)
    CompletableFuture<ServerResponse> submitJob(@RequestBody ServerRequest req) {
        CompletableFuture<ServerResponse> ret = new CompletableFuture<>();
        if (req.getDocument() == null) {
            ret.completeExceptionally(new IllegalArgumentException("A document must be provided!"));
            return ret;
        }
        if (req.getStreamName() == null) {
            ret.completeExceptionally(new IllegalArgumentException("A stream name must be provided"));
            return ret;
        }
        if (req.getSerializers() == null || req.getSerializers().isEmpty()) {
            ret.completeExceptionally(new IllegalArgumentException("At least 1 deserializer must be defined!"));
            return ret;
        }
        UIMAStream stream = getStream(req.getStreamName().toLowerCase());
        if (stream == null) {
            ret.completeExceptionally(new IllegalArgumentException("There is no currently running stream called " + req.getStreamName()));
            return ret;
        }
        final long startTime = System.currentTimeMillis();
        CompletableFuture<CAS> pipelineResult = stream.submit(req.getDocument(), req.getMetadata());
        pipelineResult.exceptionally(e -> {
            ret.completeExceptionally(e);
            return null;
        }).thenApply((cas) -> {
            try {
                Map<String, String> results = new HashMap<>();
                for (String serializerName : req.getSerializers()) {
                    UIMANLPResultSerializer serializer = getSerializer(serializerName);
                    if (serializer == null) {
                        results.put(serializerName.toLowerCase(),
                                "Illegal Argument: serializer " + serializerName.toLowerCase() + " not found!");
                    } else {
                        results.put(serializerName.toLowerCase(), serializer.serializeNLPResult(cas).toString());
                    }
                }
                ServerResponse resp = new ServerResponse(System.currentTimeMillis() - startTime,
                        req.getMetadata(), req.getDocument(), results);
                ret.complete(resp);
                return cas;
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Error occurred during pipeline serialization!", e);
                ret.completeExceptionally(e);
                return cas;
            }
        });

        return ret;
    }

    public static void main(String... args) {
        SpringApplication.run(UIMARESTServer.class);
    }
}
