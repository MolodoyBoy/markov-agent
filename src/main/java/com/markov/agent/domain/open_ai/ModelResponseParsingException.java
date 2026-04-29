package com.markov.agent.domain.open_ai;

public class ModelResponseParsingException extends RuntimeException {

    public ModelResponseParsingException(String message) {
        super(message);
    }

    public ModelResponseParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
