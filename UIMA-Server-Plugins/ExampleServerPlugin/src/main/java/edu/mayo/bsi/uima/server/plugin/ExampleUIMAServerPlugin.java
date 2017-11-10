package edu.mayo.bsi.uima.server.plugin;

import edu.mayo.bsi.uima.server.api.UIMAServerPlugin;

public class ExampleUIMAServerPlugin implements UIMAServerPlugin {
    @Override
    public String getName() {
        return "Example-Plugin";
    }
}
