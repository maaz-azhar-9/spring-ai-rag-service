package com.maazazhar.ragservice.llm;

/**
 * The only place the application talks to a language model.
 *
 * <p>Keeping this behind an interface means business logic can be
 * unit-tested without a model, and the provider can change without touching callers.
 */
public interface LlmGateway {

    /** Free-text completion. */
    String complete(String systemPrompt, String userPrompt);
}
