package com.markov.agent.data_loader.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TwelveDataTimeSeriesResponse(
    Meta meta,
    List<TimeSeriesValue> values,
    String status
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(String symbol) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimeSeriesValue(String datetime, String close) {}
}
