package com.markov.agent.domain.open_ai;

public record ModelRequest(String query, Integer daysForward) {

    private static final int MIN_DAYS_FORWARD = 1;
    private static final int MAX_DAYS_FORWARD = 12;
    private static final int MAX_QUERY_LENGTH = 100;

    public ModelRequest {
        if (daysForward == null) {
            daysForward = MIN_DAYS_FORWARD;
        }

        if (query == null || query.length() > MAX_QUERY_LENGTH) {
            throw new ModelResponseParsingException("Query must be non-null and at most " + MAX_QUERY_LENGTH + " characters long");
        }

        if (daysForward < MIN_DAYS_FORWARD || daysForward > MAX_DAYS_FORWARD) {
            throw new ModelResponseParsingException("Days Forward must be between 1 and " + MAX_DAYS_FORWARD);
        }
    }
}