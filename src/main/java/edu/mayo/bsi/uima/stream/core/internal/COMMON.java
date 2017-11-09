package edu.mayo.bsi.uima.stream.core.internal;

import org.apache.uima.cas.CAS;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains data structures shared across multiple parts of the UIMA pipeline to support streaming
 */
public class COMMON {
    /**
     * A Map of Job ID to their respective Completable Futures
     */
    public static Map<UUID, CompletableFuture<CAS>> CURR_JOBS = new ConcurrentHashMap<>();
}
