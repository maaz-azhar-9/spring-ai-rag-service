package com.maazazhar.ragservice.llm;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.maazazhar.ragservice.config.RagProperties;

/**
 * {@link LlmGateway} backed by Spring AI's {@link ChatClient}.
 *
 * <p>Every call runs on a virtual thread with a hard timeout. HTTP-client timeouts alone are not
 * enough in practice: a slow model can keep a connection alive and hang a request thread for
 * minutes. On timeout the call is cancelled and retried up to {@code rag.llm-max-attempts}.
 */
@Component
public class SpringAiLlmGateway implements LlmGateway {

    private static final Logger log = LoggerFactory.getLogger(SpringAiLlmGateway.class);

    private final ChatClient chatClient;
    private final RagProperties properties;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public SpringAiLlmGateway(ChatClient.Builder chatClientBuilder, RagProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return callWithTimeout(() -> chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content());
    }

    private <T> T callWithTimeout(Supplier<T> call) {
        Exception last = null;
        for (int attempt = 1; attempt <= properties.llmMaxAttempts(); attempt++) {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(call, executor);
            try {
                return future.get(properties.llmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                last = e;
                log.warn("LLM call timed out after {} (attempt {}/{})",
                        properties.llmTimeout(), attempt, properties.llmMaxAttempts());
            } catch (ExecutionException e) {
                last = e;
                log.warn("LLM call failed (attempt {}/{}): {}",
                        attempt, properties.llmMaxAttempts(), e.getCause().getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LlmUnavailableException("Interrupted while waiting for the model", e);
            }
        }
        throw new LlmUnavailableException("The language model is unavailable, please retry later", last);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
