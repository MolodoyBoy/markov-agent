package com.markov.agent.data_loader.optimizer;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DifferentialEvolutionTest {

    @Test
    void findsMinimumOfShiftedParabola() {
        DifferentialEvolution de = new DifferentialEvolution(20, 100, 0.7, 0.9, new Random(1));

        double[] best = de.minimize(x -> Math.pow(x[0] - 1.3, 2) + Math.pow(x[1] + 0.4, 2),
            new double[]{-5, -5}, new double[]{5, 5});

        assertThat(best[0]).isCloseTo(1.3, within(1e-3));
        assertThat(best[1]).isCloseTo(-0.4, within(1e-3));
    }

    @Test
    void staysWithinBoundsWhenMinimumLiesOutside() {
        DifferentialEvolution de = new DifferentialEvolution(10, 50, 0.7, 0.9, new Random(1));

        double[] best = de.minimize(x -> x[0], new double[]{0.5}, new double[]{2.0});

        assertThat(best[0]).isCloseTo(0.5, within(1e-6));
    }
}
