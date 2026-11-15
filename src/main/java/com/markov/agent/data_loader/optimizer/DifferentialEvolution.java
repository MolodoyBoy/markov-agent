package com.markov.agent.data_loader.optimizer;

import java.util.Random;
import java.util.function.ToDoubleFunction;

/**
 * DE/rand/1/bin minimiser over a bounded box.
 */
public class DifferentialEvolution {

    private final int populationSize;
    private final int generations;
    private final double mutationFactor;
    private final double crossoverRate;
    private final Random random;

    public DifferentialEvolution(int populationSize,
                                 int generations,
                                 double mutationFactor,
                                 double crossoverRate,
                                 Random random) {
        if (populationSize < 4) {
            throw new IllegalArgumentException("DE/rand/1 needs at least 4 individuals");
        }
        this.populationSize = populationSize;
        this.generations = generations;
        this.mutationFactor = mutationFactor;
        this.crossoverRate = crossoverRate;
        this.random = random;
    }

    public double[] minimize(ToDoubleFunction<double[]> objective, double[] lower, double[] upper) {
        int dimensions = lower.length;
        double[][] population = new double[populationSize][dimensions];
        double[] costs = new double[populationSize];

        for (int i = 0; i < populationSize; i++) {
            for (int d = 0; d < dimensions; d++) {
                population[i][d] = lower[d] + random.nextDouble() * (upper[d] - lower[d]);
            }
            costs[i] = objective.applyAsDouble(population[i]);
        }

        for (int generation = 0; generation < generations; generation++) {
            for (int i = 0; i < populationSize; i++) {
                int a = pickDistinct(i, -1, -1);
                int b = pickDistinct(i, a, -1);
                int c = pickDistinct(i, a, b);
                int forcedDimension = random.nextInt(dimensions);

                double[] trial = new double[dimensions];
                for (int d = 0; d < dimensions; d++) {
                    if (d == forcedDimension || random.nextDouble() < crossoverRate) {
                        double mutant = population[a][d] + mutationFactor * (population[b][d] - population[c][d]);
                        trial[d] = Math.clamp(mutant, lower[d], upper[d]);
                    } else {
                        trial[d] = population[i][d];
                    }
                }

                double trialCost = objective.applyAsDouble(trial);
                if (trialCost <= costs[i]) {
                    population[i] = trial;
                    costs[i] = trialCost;
                }
            }
        }

        int best = 0;
        for (int i = 1; i < populationSize; i++) {
            if (costs[i] < costs[best]) {
                best = i;
            }
        }
        return population[best];
    }

    private int pickDistinct(int exclude1, int exclude2, int exclude3) {
        int candidate;
        do {
            candidate = random.nextInt(populationSize);
        } while (candidate == exclude1 || candidate == exclude2 || candidate == exclude3);
        return candidate;
    }
}
