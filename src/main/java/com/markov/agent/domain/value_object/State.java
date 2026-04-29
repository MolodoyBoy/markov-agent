package com.markov.agent.domain.value_object;

import java.util.Map;
import java.util.Arrays;

import static java.util.stream.Collectors.toMap;
import static java.util.function.Function.identity;

public enum State {

    DOWN(1, "Stock price will decrease."),
    STABLE(2, "Stock price will remain stable."),
    UP(3, "Stock price will increase.");

    private final int value;
    private final String description;

    State(int value, String description) {
        this.value = value;
        this.description = description;
    }

    private static final Map<Integer, State> VALUE_MAP = Arrays.stream(State.values())
        .collect(toMap(State::getValue, identity()));

    public static State fromValue(int value) {
        return VALUE_MAP.get(value);
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
