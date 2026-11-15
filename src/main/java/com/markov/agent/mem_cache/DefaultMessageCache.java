package com.markov.agent.mem_cache;

import static com.markov.agent.domain.mem_cache.CachedMessage.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.markov.agent.domain.mem_cache.MessageCache;
import com.markov.agent.domain.mem_cache.CachedMessage;

@Component
class DefaultMessageCache implements MessageCache {

    private final Duration messageTTL;
    private final ConcurrentMap<Long, CachedMessage> cache;

    public DefaultMessageCache(@Value("${bot.message_cache_ttl}") Duration messageTTL) {
        this.messageTTL = messageTTL;
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public void save(long id, String message) {
        cache.computeIfPresent(id, (_, v) -> new CachedMessage(Status.PROCESSED, message, v.creationTime()));
    }

    @Override
    public CachedMessage compute(long id) {
        return cache.compute(id, (_, v) -> {
            if (v == null) {
                return createNew();
            }

            if (v.status() != Status.PROCESSED && v.creationTime().plus(messageTTL).isBefore(LocalDateTime.now())) {
                return new CachedMessage(Status.PENDING, v.actual(), v.creationTime());
            }

            if (v.status() == Status.NEW) {
                return new CachedMessage(Status.PENDING, null, v.creationTime());
            }

            return v;
        });
    }

    private CachedMessage createNew() {
        return new CachedMessage(Status.NEW, null, LocalDateTime.now());
    }
}