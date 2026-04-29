package com.markov.agent.rest_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record User(
    long id,
    @JsonProperty("is_bot") boolean isBot,
    @JsonProperty("first_name") String firstName,
    @JsonProperty("last_name") String lastName,
    String username,
    @JsonProperty("language_code") String languageCode
) {}
