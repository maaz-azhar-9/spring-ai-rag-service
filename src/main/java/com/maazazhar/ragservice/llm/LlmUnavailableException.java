package com.maazazhar.ragservice.llm;

/** Thrown when the model does not answer within the timeout after every attempt. */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
