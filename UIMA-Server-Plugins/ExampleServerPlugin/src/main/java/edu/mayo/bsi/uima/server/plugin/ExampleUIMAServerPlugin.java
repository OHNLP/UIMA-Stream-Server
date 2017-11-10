package edu.mayo.bsi.uima.server.plugin;

import edu.mayo.bsi.uima.server.api.UIMAServer;
import edu.mayo.bsi.uima.server.api.UIMAServerPlugin;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

public class ExampleUIMAServerPlugin implements UIMAServerPlugin {
    @Override
    public String getName() {
        return "Example-Plugin";
    }

    @Override
    public void onEnable(UIMAServer server) {
        try {
            server.registerStream("example", AnalysisEngineFactory.createEngineDescription(ExampleMetadataAnalysisEngine.class), AnalysisEngineFactory.createEngineDescription(ExampleMetadataAnalysisEngine.class));
        } catch (ResourceInitializationException e) {
            throw new RuntimeException(e);
        }
        server.registerSerializer("example", new ExampleSerializer());
    }
}
