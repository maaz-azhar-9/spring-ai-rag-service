package com.maazazhar.ragservice.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for retrieval and LLM calls, bound from the {@code rag.*} namespace.
 *
 * @param defaultTopK         chunks retrieved per question when the caller does not specify
 * @param similarityThreshold minimum cosine similarity for a chunk to count as relevant
 * @param chunkSize           target chunk size in tokens when splitting documents
 * @param llmTimeout          hard upper bound for a single model call
 * @param llmMaxAttempts      total attempts (first call + retries) before giving up
 */
@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        int defaultTopK,
        double similarityThreshold,
        int chunkSize,
        Duration llmTimeout,
        int llmMaxAttempts) {

    public RagProperties {
        if (defaultTopK <= 0) defaultTopK = 4;
        if (similarityThreshold <= 0) similarityThreshold = 0.35;
        if (chunkSize <= 0) chunkSize = 400;
        if (llmTimeout == null) llmTimeout = Duration.ofSeconds(30);
        if (llmMaxAttempts <= 0) llmMaxAttempts = 2;
    }
}
