package edu.mayo.bsi.uima.server.api;

import edu.mayo.bsi.uima.server.core.UIMAStreamImpl;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Represents a UIMA Stream instance that can handle streamed requests for document processing
 */
public interface UIMAStream {

    /**
     * @param name         The name of the stream
     * @param metadataDesc A descriptor for the analysis engine used to process metadata by this stream,
     *                     it is always the first analysis engine run after a request is received
     * @param pipelineDesc A descriptor for the analysis engine UIMA should use as part of the NLP pipeline
     * @return A new UIMA stream instance when initialization is completed using the given settings
     */
    static Future<UIMAStream> build(String name, AnalysisEngineDescription metadataDesc, AnalysisEngineDescription pipelineDesc) {
        return Executors.newSingleThreadExecutor().submit(() -> new UIMAStreamImpl(name, metadataDesc, pipelineDesc));
    }

    /**
     * Schedules a request to the UIMA pipeline for processing
     *
     * @param document The document to process
     * @param metadata Metadata to associate with this document
     * @return A completable future that will return a deep copy of the resulting CAS once computation is complete.
     * This returned CAS can be further manipulated on a different thread as the UIMA pipeline processing it
     */
    CompletableFuture<CAS> submit(String document, String metadata);

    /**
     * Gracefully shuts down this stream, rejecting any new requests but completing any outstanding requests. <br>
     * This request will block until all current outstanding requests are completed.
     */
    void shutdown();

    /**
     * Gracefully shuts down this stream, rejecting any new requests but completing any outstanding requests. <br>
     * Unlike {@link #shutdown()}, this request will not block the execution thread.
     *
     * @return A future object where {@link Future#get()} will unblock when shutdown completes
     */
    Future<?> shutdownAsync();

    /**
     * Force-shuts down this stream, any currently running tasks are cancelled and anything in the pipeline is
     * immediately halted.
     */
    void shutdownNow();
}
