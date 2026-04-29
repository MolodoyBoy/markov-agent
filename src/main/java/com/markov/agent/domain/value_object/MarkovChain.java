package com.markov.agent.domain.value_object;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record MarkovChain(Map<State, List<Cell>> markovRows) {

    public record Cell(State stateTo, BigDecimal probability) {}

    public MarkovChainPrediction predict(int n, MarkovChainPrediction initialPrediction) {
        Map<State, BigDecimal> current = new HashMap<>(initialPrediction.stateProbabilities());

        for (int step = 1; step < n; step++) {
            Map<State, BigDecimal> next = new HashMap<>();

            for (State toState : State.values()) {
                BigDecimal sum = BigDecimal.ZERO;
                for (State fromState : State.values()) {
                    BigDecimal fromProbability = current.getOrDefault(fromState, BigDecimal.ZERO);
                    BigDecimal transitionProbability = getTransitionProbability(fromState, toState);
                    sum = sum.add(fromProbability.multiply(transitionProbability));
                }

                next.put(toState, sum.setScale(6, RoundingMode.HALF_UP));
            }
            current = next;
        }

        return new MarkovChainPrediction(current);
    }

    private BigDecimal getTransitionProbability(State from, State to) {
        List<Cell> cells = markovRows.get(from);
        if (cells == null) {
            return BigDecimal.ZERO;
        }

        return cells.stream()
            .filter(cell -> cell.stateTo() == to)
            .map(Cell::probability)
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }
}