package com.markov.agent.rest_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramResponse(
    String method,
    @JsonProperty("chat_id") long chatId,
    String text
) {}
