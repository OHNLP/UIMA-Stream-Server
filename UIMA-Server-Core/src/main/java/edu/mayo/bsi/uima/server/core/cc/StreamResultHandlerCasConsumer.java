package edu.mayo.bsi.uima.server.core.cc;

import edu.mayo.bsi.uima.server.core.internal.COMMON;
import edu.mayo.bsi.uima.server.StreamingMetadata;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.CasCreationUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles completion of a streamed pipeline and returns the results of computation in a deep copy of the result CAS
 */
public class StreamResultHandlerCasConsumer extends JCasConsumer_ImplBase {

    @Override
    public void process(JCas cas) throws AnalysisEngineProcessException {
        StreamingMetadata meta = JCasUtil.selectSingle(cas, StreamingMetadata.class);
        if (meta == null) {
            throw new IllegalStateException("A job that wasn't enqueued properly somehow made its way into the pipeline!");
        }
        UUID jobID = UUID.fromString(meta.getJobID());
        CompletableFuture<CAS> ret = COMMON.CURR_JOBS.remove(jobID);
        if (ret == null) {
            Logger.getLogger(StreamResultHandlerCasConsumer.class.getName()).log(Level.SEVERE, jobID + " completed prematurely with no completeablefuture");
            return; // The relevant completable future for the job is already gone, this should not happen but we don't want to crash the pipeline either TODO log
        }
        try {
            Logger.getLogger(StreamResultHandlerCasConsumer.class.getName()).log(Level.SEVERE, jobID + " completed! Creating copy");
            CAS dest = CasCreationUtils.createCas(TypeSystemDescriptionFactory.createTypeSystemDescription(), null, null);
            CasCopier.copyCas(cas.getCas(), dest, true, false);
            ret.complete(dest);
        } catch (Throwable e) {
            ret.completeExceptionally(e);
        }
    }

}
