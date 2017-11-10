package edu.mayo.bsi.uima.server.plugin;

import edu.mayo.bsi.uima.server.StreamingMetadata;
import edu.mayo.bsi.uima.server.api.UIMANLPResultSerializer;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.Serializable;

/**
 * An example simplistic serializer that simply returns the assigned job ID, text, and processed metadata
 */
public class ExampleSerializer implements UIMANLPResultSerializer {
    @Override
    public Serializable serializeNLPResult(CAS cas) {
        try {
            JCas jcas = cas.getJCas();
            StreamingMetadata meta = JCasUtil.selectSingle(jcas, StreamingMetadata.class);
            return meta.getJobID() + jcas.getDocumentText() + " " + meta.getMetadata();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
