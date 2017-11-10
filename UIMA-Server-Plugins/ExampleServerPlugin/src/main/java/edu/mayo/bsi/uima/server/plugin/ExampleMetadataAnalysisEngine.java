package edu.mayo.bsi.uima.server.plugin;

import edu.mayo.bsi.uima.server.StreamingMetadata;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

/**
 * An example metadata analysis engine that simply retrieves the metadata and adds "processed"
 * to the end of the value. We use this twice for both metadata processing and pipeline (so would expect
 * to see two "processed" appended as a result)
 */
public class ExampleMetadataAnalysisEngine extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas cas) throws AnalysisEngineProcessException {
        StreamingMetadata meta = JCasUtil.selectSingle(cas, StreamingMetadata.class);
        if (meta == null) {
            // A guarantee is made by UIMA server that a single StreamingMetadata instance will always be present
            throw new AssertionError("A job was submitted without a meta!");
        }
        meta.setMetadata(meta.getMetadata() == null ?  "processed" : meta.getMetadata() + " processed");
    }
}
