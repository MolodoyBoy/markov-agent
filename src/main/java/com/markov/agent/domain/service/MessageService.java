package com.markov.agent.domain.service;

import com.markov.agent.domain.mem_cache.CachedMessage;
import com.markov.agent.domain.mem_cache.MessageCache;
import com.markov.agent.rest_api.dto.Update;
import com.markov.agent.domain.source.AIClient;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class MessageService {

    private static final Logger logger = getLogger(MessageService.class);

    private static final String TOO_LONG_MESSAGE = "Sorry, your message is too long.";
    private static final String GREETING_MESSAGE_RESPONSE = "Hi! I'm a Markov agent. How can I help you?";

    private final AIClient aiClient;
    private final int maxMessageLength;
    private final MessageCache messageCache;

    public MessageService(AIClient aiClient,
                          MessageCache messageCache,
                          @Value("${bot.max_message_length}") int maxMessageLength) {
        this.aiClient = aiClient;
        this.messageCache = messageCache;
        this.maxMessageLength = maxMessageLength;
    }

    public String processMessage(Update update) {
        CachedMessage cachedMessage = messageCache.compute(update.updateId());
        if (cachedMessage.new_()) {
            return processNewMessage(update);
        }

        // Exception thrown in case if api is retried but message from AI not received yet.
        if (cachedMessage.pending()) {
            logger.warn("Message with id {} is in process.", update.updateId());
            throw WebClientResponseException.create(409, "Message is processing", null, null, null);
        }

        logger.info("Get message with id {} from cache.", update.updateId());

       return cachedMessage.actual();
    }

    private String processNewMessage(Update update) {

        logger.info("Received message with id: {}", update.updateId());

        String text = update.text();

        if (text.length() > maxMessageLength) {
            return TOO_LONG_MESSAGE;
        }

        if (text.equals("/start")) {
            return GREETING_MESSAGE_RESPONSE;
        }

        return aiClient.getResponse(update.updateId(), text);
    }
}