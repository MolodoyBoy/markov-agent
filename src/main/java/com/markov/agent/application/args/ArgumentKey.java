package com.markov.agent.application.args;

public enum ArgumentKey {
    WEBHOOK("webhook");

    private final String key;

    ArgumentKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
