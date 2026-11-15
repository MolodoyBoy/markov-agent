package com.markov.agent.domain.mem_cache;

import java.time.LocalDateTime;

public record CachedMessage(Status status, String actual, LocalDateTime creationTime) {

    public boolean new_() {
        return status == Status.NEW;
    }

    public boolean pending() {
        return status == Status.PENDING;
    }

    public enum Status {
        NEW,
        PENDING,
        PROCESSED
    }
}

