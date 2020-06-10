package edu.mayo.dhs.uima.server.api;

/**
 * Represents a UIMA server plugin, which adds functionality to a running UIMA server such as
 * adding new streamed pipelines and serializers.
 *
 * All packaged plugins must include a plugin-class.info containing lines pointing to implementations
 * within that JAR
 */
public interface UIMAServerPlugin {
    /**
     * @return The name of this plugin, which is used for a variety of identification tasks
     */
    String getName();

    /**
     * Runs when the server enables the plugin (after all plugins are added to the classpath)
     * Tasks such as stream and serializer registration should be done at this time.
     * <p>
     * No guarantee is made on the synchronicity of the loading process, nor on the order
     * in which plugins are enabled
     * @param server The server instance performing the initialization
     */
    void onEnable(UIMAServer server);
}
