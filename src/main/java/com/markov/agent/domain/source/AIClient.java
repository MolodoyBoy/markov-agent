package com.markov.agent.domain.source;

public interface AIClient {

    String getResponse(long id, String request);
}