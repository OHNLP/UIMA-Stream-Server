package edu.mayo.dhs.uima.server.api;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;

/**
 * Represents a UIMA Server instance, which are online services that can manage multiple {@link UIMAStream} and
 * communicate with clients to receive requests, map them to the appropriate stream, and return a response to the
 * requesting client
 */
public interface UIMAServer {

    /**
     * Called on server bootup, initialization tasks that depend on a UIMA server and/or other plugins should not be
     * called in the constructor, but rather should be called here.
     */
    void start();

    /**
     * Gets a {@link UIMAServerPlugin}, if loaded by the server
     *
     * @param pluginName The name of the plugin as declared in its implementation
     * @return The plugin, if it is loaded and present, null otherwise
     */
    UIMAServerPlugin getPlugin(String pluginName);

    /**
     * Retrieves a stream by name
     *
     * @param streamName The name of the {@link UIMAStream}
     * @return The stream if registered, null otherwise
     */
    UIMAStream getStream(String streamName);

    /**
     * Registers a stream by name
     *
     * @param streamName   The name of the stream
     * @param metadataDesc (Optional) A descriptor for the analysis engine used to process metadata by this stream,
     *                     it is always the first analysis engine run after a request is received
     * @param pipelineDesc A descriptor for the analysis engine UIMA should use as part of the NLP pipeline
     * @return The registered stream if successful. Throws a subclass of {@link RuntimeException} otherwise
     */
    UIMAStream registerStream(String streamName, AnalysisEngineDescription metadataDesc, AnalysisEngineDescription pipelineDesc);

    /**
     * Retrieves a Serializer by name
     *
     * @param serializerName The name of the {@link UIMANLPResultSerializer}
     * @return The serializer if registered, null otherwise
     */
    UIMANLPResultSerializer getSerializer(String serializerName);

    void registerSerializer(String streamName, UIMANLPResultSerializer serializer);
}
