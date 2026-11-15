package com.markov.agent.data_loader.optimizer;

import com.markov.agent.domain.value_object.DailyStock;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Random;

import static java.util.Comparator.comparing;

/**
 * Finds the scale k of the DOWN/STABLE/UP thresholds (AVG ± k × STD_DEV) that makes the
 * transition matrix the best out-of-sample one-day predictor, scored by macro-F1 over all three
 * states: plain accuracy and Cohen's kappa both reward scales that merge or empty a state.
 */
@Service
public class ScaleOptimizer {

    private static final double MIN_SCALE = 0.01;
    private static final double MAX_SCALE = 2.0;
    private static final double TRAIN_SHARE = 0.8;

    private static final int POPULATION_SIZE = 20;
    private static final int GENERATIONS = 60;
    private static final double MUTATION_FACTOR = 0.7;
    private static final double CROSSOVER_RATE = 0.9;
    private static final long SEED = 42L;

    private static final int DOWN = 0;
    private static final int STABLE = 1;
    private static final int UP = 2;
    private static final int STATES = 3;

    public OptionalDouble optimize(Map<Integer, List<DailyStock>> dailyStockReturns, double avg, double stdDev) {
        List<Transition> transitions = toTransitions(dailyStockReturns.values());
        if (transitions.isEmpty()) {
            return OptionalDouble.empty();
        }

        long cutoffEpochDay = cutoffEpochDay(transitions);
        List<Transition> train = transitions.stream().filter(t -> t.epochDay() < cutoffEpochDay).toList();
        List<Transition> test = transitions.stream().filter(t -> t.epochDay() >= cutoffEpochDay).toList();
        if (train.isEmpty() || test.isEmpty()) {
            return OptionalDouble.empty();
        }

        DifferentialEvolution differentialEvolution = new DifferentialEvolution(
            POPULATION_SIZE, GENERATIONS, MUTATION_FACTOR, CROSSOVER_RATE, new Random(SEED));

        double[] best = differentialEvolution.minimize(
            candidate -> -score(train, test, avg, stdDev, candidate[0]),
            new double[]{MIN_SCALE},
            new double[]{MAX_SCALE}
        );

        return OptionalDouble.of(best[0]);
    }

    double score(List<Transition> train, List<Transition> test, double avg, double stdDev, double scale) {
        double lower = avg - scale * stdDev;
        double upper = avg + scale * stdDev;

        long[][] transitionCounts = new long[STATES][STATES];
        for (Transition transition : train) {
            transitionCounts[classify(transition.from(), lower, upper)][classify(transition.to(), lower, upper)]++;
        }

        int[] predictedNext = new int[STATES];
        for (int from = 0; from < STATES; from++) {
            predictedNext[from] = mostProbable(transitionCounts[from]);
        }

        long[][] confusion = new long[STATES][STATES];
        for (Transition transition : test) {
            int actual = classify(transition.to(), lower, upper);
            int predicted = predictedNext[classify(transition.from(), lower, upper)];
            confusion[actual][predicted]++;
        }

        return macroF1(confusion);
    }

    private static int classify(double value, double lower, double upper) {
        if (value < lower) {
            return DOWN;
        }
        if (value > upper) {
            return UP;
        }
        return STABLE;
    }

    private static int mostProbable(long[] row) {
        int best = STABLE;
        for (int to = 0; to < STATES; to++) {
            if (row[to] > row[best]) {
                best = to;
            }
        }
        return best;
    }

    private static double macroF1(long[][] confusion) {
        double sum = 0;
        for (int state = 0; state < STATES; state++) {
            long actualTotal = 0;
            long predictedTotal = 0;
            for (int other = 0; other < STATES; other++) {
                actualTotal += confusion[state][other];
                predictedTotal += confusion[other][state];
            }
            long denominator = actualTotal + predictedTotal;
            if (denominator > 0) {
                sum += 2.0 * confusion[state][state] / denominator;
            }
        }
        return sum / STATES;
    }

    // Mirrors updateMarkovChain in DbMarkovChainSource: only consecutive calendar days form a transition.
    static List<Transition> toTransitions(Collection<List<DailyStock>> returnsByCompany) {
        List<Transition> transitions = new ArrayList<>();
        for (List<DailyStock> companyReturns : returnsByCompany) {
            List<DailyStock> sorted = companyReturns.stream().sorted(comparing(DailyStock::stockDate)).toList();
            for (int i = 0; i + 1 < sorted.size(); i++) {
                DailyStock current = sorted.get(i);
                DailyStock next = sorted.get(i + 1);
                LocalDate expectedNext = current.stockDate().plusDays(1);
                if (next.stockDate().equals(expectedNext)) {
                    transitions.add(new Transition(
                        current.stockDate().toEpochDay(),
                        current.closeValue().doubleValue(),
                        next.closeValue().doubleValue()
                    ));
                }
            }
        }
        return transitions;
    }

    private static long cutoffEpochDay(List<Transition> transitions) {
        long[] days = transitions.stream().mapToLong(Transition::epochDay).distinct().sorted().toArray();
        int index = (int) Math.floor(days.length * TRAIN_SHARE);
        return days[Math.min(index, days.length - 1)];
    }

    record Transition(long epochDay, double from, double to) {
    }
}
