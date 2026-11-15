package com.markov.agent.data_loader.optimizer;

import com.markov.agent.domain.value_object.DailyStock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ScaleOptimizerTest {

    private static final LocalDate START = LocalDate.of(2024, 1, 1);

    private final ScaleOptimizer optimizer = new ScaleOptimizer();

    @Test
    void findsScaleSeparatingStickyRegimes() {
        Map<Integer, List<DailyStock>> returns = stickyRegimes();
        double[] stats = meanAndSampleStdDev(returns);
        List<ScaleOptimizer.Transition> transitions = ScaleOptimizer.toTransitions(returns.values());
        List<ScaleOptimizer.Transition> train = transitions.subList(0, transitions.size() / 2);
        List<ScaleOptimizer.Transition> test = transitions.subList(transitions.size() / 2, transitions.size());

        OptionalDouble scale = optimizer.optimize(returns, stats[0], stats[1]);

        assertThat(scale).isPresent();
        double lower = stats[0] - scale.getAsDouble() * stats[1];
        double upper = stats[0] + scale.getAsDouble() * stats[1];
        assertThat(lower).isBetween(-0.03, -0.005);
        assertThat(upper).isBetween(0.005, 0.03);
        assertThat(optimizer.score(train, test, stats[0], stats[1], scale.getAsDouble())).isGreaterThan(0.8);
    }

    @Test
    void scaleThatPutsEveryReturnIntoStableScoresOneThird() {
        Map<Integer, List<DailyStock>> returns = stickyRegimes();
        double[] stats = meanAndSampleStdDev(returns);
        List<ScaleOptimizer.Transition> transitions = ScaleOptimizer.toTransitions(returns.values());

        double score = optimizer.score(transitions, transitions, stats[0], stats[1], 100.0);

        assertThat(score).isCloseTo(1.0 / 3, within(1e-9));
    }

    @Test
    void ignoresPairsThatAreNotConsecutiveCalendarDays() {
        List<DailyStock> company = List.of(
            stock(START, "0.01"),
            stock(START.plusDays(1), "0.02"),
            stock(START.plusDays(4), "0.03")
        );

        List<ScaleOptimizer.Transition> transitions = ScaleOptimizer.toTransitions(List.of(company));

        assertThat(transitions).containsExactly(new ScaleOptimizer.Transition(START.toEpochDay(), 0.01, 0.02));
    }

    @Test
    void returnsEmptyWithoutTransitions() {
        Map<Integer, List<DailyStock>> returns = Map.of(1, List.of(stock(START, "0.01")));

        assertThat(optimizer.optimize(returns, 0.0, 0.01)).isEmpty();
    }

    private static Map<Integer, List<DailyStock>> stickyRegimes() {
        Random random = new Random(7);
        Map<Integer, List<DailyStock>> returns = new HashMap<>();
        for (int companyId = 1; companyId <= 5; companyId++) {
            List<DailyStock> companyReturns = new ArrayList<>();
            int regime = 1;
            for (int day = 0; day < 400; day++) {
                if (random.nextDouble() > 0.9) {
                    regime = random.nextInt(3);
                }
                double value = switch (regime) {
                    case 0 -> -0.03;
                    case 2 -> 0.03;
                    default -> (random.nextDouble() - 0.5) * 0.01;
                };
                companyReturns.add(stock(START.plusDays(day), String.valueOf(value)));
            }
            returns.put(companyId, companyReturns);
        }
        return returns;
    }

    private static double[] meanAndSampleStdDev(Map<Integer, List<DailyStock>> returns) {
        double[] values = returns.values().stream()
            .flatMap(List::stream)
            .mapToDouble(s -> s.closeValue().doubleValue())
            .toArray();
        double mean = 0;
        for (double v : values) {
            mean += v;
        }
        mean /= values.length;
        double sumSquares = 0;
        for (double v : values) {
            sumSquares += (v - mean) * (v - mean);
        }
        return new double[]{mean, Math.sqrt(sumSquares / (values.length - 1))};
    }

    private static DailyStock stock(LocalDate date, String value) {
        return new DailyStock(1, new BigDecimal(value), date);
    }
}
