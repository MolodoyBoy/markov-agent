package com.markov.agent.domain.client;

import com.markov.agent.application.event.SetWebhookEvent;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.slf4j.LoggerFactory.*;

@Component
public class TelegramWebhookClient {

    private final Logger logger = getLogger(TelegramWebhookClient.class);

    private final RestClient restClient;

    public TelegramWebhookClient(@Value("${bot.updates_url}") String updatesUrl,
                                 @Value("${external_api.telegram_api.bot.url}") String baseUrl) {
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultUriVariables(Map.of(
                "url", updatesUrl
            ))
            .build();
    }

    @EventListener(SetWebhookEvent.class)
    public void setWebhook() {
        logger.info("Try to set webhook...");

        try {
            String response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("url", "{url}")
                    .build()
                )
                .retrieve()
                .body(String.class);

            logger.info("Webhook response: {}", response);
        } catch (Exception e) {
            logger.error("Failed to set webhook. {}", e.getMessage());
        }
    }
}