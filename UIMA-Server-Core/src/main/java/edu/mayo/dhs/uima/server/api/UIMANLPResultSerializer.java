package edu.mayo.dhs.uima.server.api;


import com.fasterxml.jackson.databind.JsonNode;
import org.apache.uima.cas.CAS;

import java.io.Serializable;

public interface UIMANLPResultSerializer {
    /**
     * Serializes (i.e. translate into a format that can be shared/stored) a NLP result that is then passed to the
     * owning UIMA server's request handler
     * @param cas The UIMA {@link CAS} that is the result of passing a document through a UIMA pipeline
     * @return A JSON object generated from NLP results
     */
    JsonNode serializeNLPResult(CAS cas);
}
