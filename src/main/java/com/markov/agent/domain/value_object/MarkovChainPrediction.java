package com.markov.agent.domain.value_object;

import java.math.BigDecimal;
import java.util.Map;

public record MarkovChainPrediction(Map<State, BigDecimal> stateProbabilities) {

    public State getMostProbableState() {
        return stateProbabilities.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
}