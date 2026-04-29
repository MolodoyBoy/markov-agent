package com.markov.agent.data_loader.source;

public interface LockSource {

    void acquireLock(int lockValue);
}
