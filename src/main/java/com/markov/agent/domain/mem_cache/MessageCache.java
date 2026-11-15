package com.markov.agent.domain.mem_cache;

public interface MessageCache {

    void save(long id, String message);

    CachedMessage compute(long id);
}