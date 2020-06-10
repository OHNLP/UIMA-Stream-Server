package edu.mayo.dhs.uima.server.core.cr;


import edu.mayo.dhs.uima.server.core.cc.StreamResultHandlerCasConsumer;
import edu.mayo.dhs.uima.server.core.internal.COMMON;
import edu.mayo.dhs.uima.server.StreamingMetadata;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A collection reader implementation for UIMA that supports streamed (live) input; will continuously wait
 * until an element is available for processing in a shared queue and block otherwise, <br>
 * Jobs can be submitted via {@link #submitMessage(String, java.util.UUID, String, String)}. <br>
 * <br>
 * Streams will be shutdown upon calls to {@link #shutdownQueue()}, at which point the queue will cease accepting new items
 * and all consumer threads will quit after processing the final document in the queue.
 */
public class BlockingStreamCollectionReader extends JCasCollectionReader_ImplBase {

    public static final String PARAM_QUEUENAME = "QUEUE_NAME";

    // TODO this does not support multiple streams (only multiple instances of the same stream)
    private static final Map<String, BlockingDeque<Job>> PROCESSING_QUEUES = new ConcurrentHashMap<>();
    private static final AtomicBoolean STREAM_OPEN = new AtomicBoolean(true);
    private Job CURRENT_WORK = null;
    private BlockingDeque<Job> PROCESSING_QUEUE;
    @ConfigurationParameter(name = PARAM_QUEUENAME)
    private String QUEUE_NAME;


    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        synchronized (PROCESSING_QUEUES) {
            PROCESSING_QUEUE = PROCESSING_QUEUES.computeIfAbsent(QUEUE_NAME, (s) -> new LinkedBlockingDeque<>());
        }
    }

    public void getNext(JCas jCas) throws IOException, CollectionException {
        jCas.setDocumentText(CURRENT_WORK.text);
        StreamingMetadata meta = new StreamingMetadata(jCas);
        meta.setJobID(CURRENT_WORK.id.toString());
        if (CURRENT_WORK.metadata != null) {
            meta.setMetadata(CURRENT_WORK.metadata);
        }
        meta.addToIndexes();
    }

    /**
     * This method is used by UIMA to determine when to shut down the pipeline. In normal operation, this method will
     * block until such a time as work is available in the queue, at which point it will retrieve a single item and
     * return true, indicating to UIMA that it should proceed to processing
     *
     * @return True when a document becomes available for processing and is reserved for the thread running the calling
     * pipeline, false if there is no more work to be retrieved and {@link #shutdownQueue()} has been called
     */
    @Override
    public boolean hasNext() {
        synchronized (PROCESSING_QUEUES) {
            while ((((CURRENT_WORK = PROCESSING_QUEUE.pollFirst())) == null) && STREAM_OPEN.get()) {
                try {
                    PROCESSING_QUEUES.wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return CURRENT_WORK != null;
        }
    }

    /**
     * Submits a document to a UIMA pipeline for processing. Will block if the NLP queue is currently full until the
     * task is successfully submitted into the queue
     *
     * @param name     The queue to submit this job to
     * @param jobID    A unique Job ID associated with this job
     * @param doc      The document to process through the UIMA pipeline
     * @param metadata A string representation of any metadata to associate with the document, that can be
     *                 manipulated and/or loaded by a subsequent annotator
     * @return A {@link CompletableFuture} object that contains methods to check on the completion
     * of this request as well as the result. The CAS stored within this future object will be a deep copy of the CAS at
     * the stage in which {@link StreamResultHandlerCasConsumer} is ran.<br>
     * <br>
     * A copy is returned as UIMA recycles CAS objects for sequential
     * reads and as such consistency of data for asynchronous operations cannot be guaranteed if the original CAS is
     * returned instead.
     * @throws IllegalStateException    If the UIMA stream pipeline has been shut down
     * @throws IllegalArgumentException if another job with the same ID already exists in the queue
     */
    public static CompletableFuture<CAS> submitMessage(String name, UUID jobID, String doc, String metadata) {
        if (!STREAM_OPEN.get()) {
            throw new IllegalStateException("Trying to submit a message for processing to a closed queue");
        } else {
            boolean successfulSubmit = false;
            Job j = new Job(doc, jobID, metadata);
            CompletableFuture<CAS> ret = new CompletableFuture<>();
            if (COMMON.CURR_JOBS.put(jobID, ret) != null) {
                throw new IllegalStateException("Submitted a job with a duplicate job ID!");
            }
            while (!successfulSubmit) {
                try {
                    final BlockingDeque<Job> queue = PROCESSING_QUEUES
                            .computeIfAbsent(name, (s) -> new LinkedBlockingDeque<>());
                    successfulSubmit = queue.offer(j, 1000, TimeUnit.MILLISECONDS);
                    synchronized (PROCESSING_QUEUES) {
                        PROCESSING_QUEUES.notifyAll();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return ret;
        }
    }

    /**
     * @return An empty progress instance - no real progress metric can be shown in the case of a stream
     */
    public Progress[] getProgress() {
        return new Progress[0];
    }

    private static boolean isShutdown() {
        return !STREAM_OPEN.get();
    }

    /**
     * @return True if the threads used by this stream should be shut down - that is to say the stream is no longer
     * accepting new jobs AND has processed all existing jobs
     */
    public boolean shouldTerminateThread() {
        return isShutdown() && PROCESSING_QUEUES.getOrDefault(QUEUE_NAME, new LinkedBlockingDeque<>()).size() == 0;
    }

    /**
     * Begins the shutdown process for this queue. The queue will no longer accept new jobs, although it will wait
     * until all current enqueued jobs are processed before triggering thread termination
     *
     * @throws IllegalStateException If the queue has already been shut down
     */
    public static void shutdownQueue() {
        if (!STREAM_OPEN.getAndSet(false)) {
            throw new IllegalStateException("Shutting down an already shut down queue");
        }
        synchronized (PROCESSING_QUEUES) {
            PROCESSING_QUEUES.notifyAll();
        }
    }


    private static class Job {
        String text;
        UUID id;
        String metadata;

        Job(String text, UUID id, String metadata) {
            this.text = text;
            this.id = id;
            this.metadata = metadata;
        }
    }
}
