package edu.mayo.bsi.uima.server.api;


import org.apache.uima.cas.CAS;

import java.io.Serializable;

public interface UIMANLPResultSerializer {
    /**
     * Serializes (i.e. translate into a format that can be shared/stored) a NLP result that is then passed to the
     * owning UIMA server's request handler
     * @param cas The UIMA {@link CAS} that is the result of passing a document through a UIMA pipeline
     * @return A serializable object generated from NLP results
     */
    Serializable serializeNLPResult(CAS cas);
}
