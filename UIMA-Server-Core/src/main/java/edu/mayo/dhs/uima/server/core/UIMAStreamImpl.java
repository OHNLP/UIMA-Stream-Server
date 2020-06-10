package edu.mayo.dhs.uima.server.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import edu.mayo.dhs.uima.server.StreamingMetadata;
import edu.mayo.dhs.uima.server.api.UIMAStream;
import edu.mayo.dhs.uima.server.core.cc.StreamResultHandlerCasConsumer;
import edu.mayo.dhs.uima.server.core.cr.BlockingStreamCollectionReader;
import edu.mayo.dhs.uima.server.core.internal.COMMON;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.fit.util.LifeCycleUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.CasCreationUtils;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class UIMAStreamImpl implements UIMAStream {

    private Logger logger;
    private String name;
    private ExecutorService threadPool;

    public UIMAStreamImpl(String streamName, AnalysisEngineDescription metadataDesc, AnalysisEngineDescription pipelineDesc) throws ResourceInitializationException {
        logger = Logger.getLogger("UIMA-Stream-" + streamName);
        name = streamName;
        int numPipelines = 1;
        String threadProp;
        if ((threadProp = System.getProperty("uima.streams.%pipeline%.threads".replace("%pipeline%", name))) == null) {
            logger.log(Level.WARNING, "The number of pipeline threads for this stream was not set via " +
                    "-Duima.server.%pipeline%.threads. Please set the value of this property as your CPU allows "
                            .replace("%pipeline%", name) + "to improve performance");
        } else {
            try {
                numPipelines = Integer.valueOf(threadProp);
            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE, "The number of pipeline threads to run set in " +
                        "-Duima.server.%pipeline%.threads, ".replace("%pipeline%", name) + threadProp + ", could not be parsed as an integer.");
            }
        }
        logger.log(Level.INFO, "Starting UIMA Stream " + name + " with " + numPipelines + " pipeline threads");

        // We don't really need to use a thread pool for this initial application, but it is included as there is
        // no real overhead cost and is way easier to expand on in the future
        threadPool = Executors.newFixedThreadPool(numPipelines, new ThreadFactoryBuilder().setNameFormat("UIMA-" + streamName + "-%d").build());
        try {
            CollectionReaderDescription STREAM_READER_DESC
                    = CollectionReaderFactory.createReaderDescription(BlockingStreamCollectionReader.class,
                    BlockingStreamCollectionReader.PARAM_QUEUENAME, name);
            AggregateBuilder pipelineBuilder = new AggregateBuilder();
            if (metadataDesc != null) {
                pipelineBuilder.add(metadataDesc);
            }
            pipelineBuilder.add(pipelineDesc);
            pipelineBuilder.add(AnalysisEngineFactory.createEngineDescription(StreamResultHandlerCasConsumer.class));
            for (int i = 0; i < numPipelines; i++) {
                initPipeline(threadPool, STREAM_READER_DESC, pipelineBuilder.createAggregateDescription());
            }
        } catch (Throwable e) {
            threadPool.shutdownNow();
            throw e;
        }
    }


    private void initPipeline(ExecutorService threadPool, CollectionReaderDescription STREAM_READER_DESC, AnalysisEngineDescription PIPELINE_DESC) {
        threadPool.submit(() -> {
            try {
                runPipeline(
                        STREAM_READER_DESC,
                        PIPELINE_DESC);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Error during pipeline operation", e);
                initPipeline(threadPool, STREAM_READER_DESC, PIPELINE_DESC);
            }
        });
    }

    /**
     * Clone of {@link SimplePipeline#runPipeline(CollectionReader, AnalysisEngine...)}
     * with added exception handling to complete futures exceptionally if an error is encountered
     */
    public static void runPipeline(final CollectionReaderDescription readerDesc,
                                   final AnalysisEngineDescription... descs) throws UIMAException, IOException {

        CollectionReader reader = null;
        AnalysisEngine aae = null;
        try {
            ResourceManager resMgr = ResourceManagerFactory.newResourceManager();

            // Create the components
            reader = UIMAFramework.produceCollectionReader(readerDesc, resMgr, null);

            // Create AAE
            final AnalysisEngineDescription aaeDesc = createEngineDescription(descs);

            // Instantiate AAE
            aae = UIMAFramework.produceAnalysisEngine(aaeDesc, resMgr, null);

            // Create CAS from merged metadata
            final CAS cas = CasCreationUtils.createCas(asList(reader.getMetaData(), aae.getMetaData()),
                    null, resMgr);
            reader.typeSystemInit(cas.getTypeSystem());

            // Process
            while (reader.hasNext()) {
                reader.getNext(cas);
                try {
                    aae.process(cas);
                } catch (Throwable e) {
                    StreamingMetadata meta = JCasUtil.selectSingle(cas.getJCas(), StreamingMetadata.class);
                    if (meta == null) {
                        return;
                    }
                    UUID jobID = UUID.fromString(meta.getJobID());
                    CompletableFuture<CAS> ret = COMMON.CURR_JOBS.remove(jobID);
                    if (ret == null) {
                        return; // The relevant completable future for the job is already gone, this should not happen but we don't want to crash the pipeline either TODO log
                    }
                    ret.completeExceptionally(e);
                }
                cas.reset();
            }

            // Signal end of processing
            aae.collectionProcessComplete();
        } finally {
            // Destroy
            LifeCycleUtil.destroy(reader);
            LifeCycleUtil.destroy(aae);
        }
    }


    @Override
    public CompletableFuture<CAS> submit(String document, String metadata) {
        return BlockingStreamCollectionReader.submitMessage(name, UUID.randomUUID(), document, metadata);
    }

    @Override
    public void shutdown() {
        logger.log(Level.INFO, name + " UIMA Stream is no longer accepting new requests");
        BlockingStreamCollectionReader.shutdownQueue();
        threadPool.shutdown();
        while (true) {
            try {
                if (threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.log(Level.INFO, "All UIMA Pipelines for UIMA Stream " + name + " have been shut down");
                    break;
                }

            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Shutdown interrupted for UIMA Stream " + name, e);
            }
        }
    }

    @Override
    public Future<?> shutdownAsync() {
        logger.log(Level.INFO, name + " UIMA Stream is no longer accepting new requests");
        BlockingStreamCollectionReader.shutdownQueue();
        threadPool.shutdown();
        return Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                try {
                    if (threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                        logger.log(Level.INFO, "All UIMA Pipelines for UIMA Stream " + name + " have been shut down");
                        return;
                    }
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Shutdown interrupted for UIMA Stream " + name, e);
                }
            }
        });
    }

    @Override
    public void shutdownNow() {
        logger.log(Level.INFO, "Force shutting down UIMA stream " + name);
        threadPool.shutdownNow();
    }
}
