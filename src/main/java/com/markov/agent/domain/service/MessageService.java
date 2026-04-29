package com.markov.agent.domain.service;

import com.markov.agent.rest_api.dto.Message;
import com.markov.agent.rest_api.dto.Update;
import com.markov.agent.domain.source.AIClient;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class MessageService {

    private static final Logger logger = getLogger(MessageService.class);

    private static final int MAX_MESSAGE_LENGTH = 30;
    private static final String GREETING_MESSAGE_RESPONSE = "Hi! I'm a Markov agent. How can I help you?";

    private final AIClient aiClient;

    public MessageService(AIClient aiClient) {
        this.aiClient = aiClient;
    }

    public String processMessage(Update update) {
        Message message = update.message();

        String text = message.text();

        logger.info("Received message: {}", text);

        if (text.length() > MAX_MESSAGE_LENGTH) {
            return "Sorry, your message is too long.";
        }

        if (text.equals("/start")) {
            return GREETING_MESSAGE_RESPONSE;
        }

        return aiClient.getResponse(text);
    }
}