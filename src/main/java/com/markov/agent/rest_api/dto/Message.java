package com.markov.agent.rest_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Message(
    @JsonProperty("message_id") long messageId,
    User from,
    long date,
    String text
) {}
