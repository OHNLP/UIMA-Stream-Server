package edu.mayo.bsi.uima.stream.api;

/**
 * Represents a UIMA Server instance, which are online services that can manage multiple {@link UIMAStream} and
 * communicate with clients to receive requests, map them to the appropriate stream, and return a response to the
 * requesting client
 */
public interface UIMAServer {

    /**
     * Called on server bootup, initialization tasks should not be done as part of the constructor
     */
    void start();
    UIMAServerPlugin getPlugin(String pluginName);
}
