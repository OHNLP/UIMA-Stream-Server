package edu.mayo.bsi.uima.stream.api;

import org.apache.uima.cas.CAS;

import java.io.Serializable;

public interface UIMAServerPlugin {
    /**
     * @return The name of this plugin, which is used for a variety of identification tasks
     */
    String getName();
    /**
     * Serializes (i.e. translate into a format that can be shared/stored) a NLP result that is then passed to the
     * owning UIMA server's request handler
     * @param cas The UIMA {@link CAS} that is the result of passing a document through a UIMA pipeline
     * @return A serializable object generated from NLP results
     */
    Serializable serializeNLPResult(CAS cas);
}
