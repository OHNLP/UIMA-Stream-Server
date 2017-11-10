package edu.mayo.bsi.uima.stream.api;

import org.apache.uima.cas.CAS;

import java.io.Serializable;

public interface UIMAServerPlugin {
    /**
     * @return The name of this plugin, which is used for a variety of identification tasks
     */
    String getName();

}
