package com.markov.agent.data_loader.importer;

import com.markov.agent.data_loader.optimizer.ScaleOptimizer;
import com.markov.agent.data_loader.source.DailyStockReturnSource;
import com.markov.agent.data_loader.source.LockSource;
import com.markov.agent.domain.source.MarkovChainSource;
import com.markov.agent.domain.value_object.MathIndex;
import com.markov.agent.domain.value_object.State;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.OptionalDouble;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class MarkovChainImporter implements Importer {

    private static final Logger logger = getLogger(MarkovChainImporter.class);

    private static final int LOCK_KEY = 1;
    private static final int MONTH_OFFSET = 36;

    private final LockSource lockSource;
    private final ScaleOptimizer scaleOptimizer;
    private final MarkovChainSource markovChainSource;
    private final DailyStockReturnSource dailyStockReturnSource;

    public MarkovChainImporter(LockSource lockSource,
                               ScaleOptimizer scaleOptimizer,
                               MarkovChainSource markovChainSource,
                               DailyStockReturnSource dailyStockReturnSource) {
        this.lockSource = lockSource;
        this.scaleOptimizer = scaleOptimizer;
        this.markovChainSource = markovChainSource;
        this.dailyStockReturnSource = dailyStockReturnSource;
    }

    @Override
    public void startImport() {
        lockSource.acquireLock(LOCK_KEY);

        logger.info("Start marcov chain import.");

        LocalDateTime fromDate = dateOffset();

        for (MathIndex mathIndex : MathIndex.values()) {
            markovChainSource.updateMathIndex(fromDate, mathIndex);
        }

        updateScale(fromDate);

        for (State from : State.values()) {
            for (State to : State.values()) {
                markovChainSource.updateMarkovChain(from, to, fromDate);
            }
        }

        logger.info("Finish marcov chain import.");
    }

    private void updateScale(LocalDateTime fromDate) {
        BigDecimal avg = markovChainSource.getMathIndex(MathIndex.AVG);
        BigDecimal stdDev = markovChainSource.getMathIndex(MathIndex.STD_DEV);
        if (avg == null || stdDev == null) {
            logger.warn("Math indices are missing, keeping the current scale.");
            return;
        }

        OptionalDouble scale = scaleOptimizer.optimize(
            dailyStockReturnSource.getDailyStockReturns(fromDate),
            avg.doubleValue(),
            stdDev.doubleValue()
        );
        if (scale.isEmpty()) {
            logger.warn("Not enough transitions to optimise the scale, keeping the current scale.");
            return;
        }

        BigDecimal rounded = BigDecimal.valueOf(scale.getAsDouble()).setScale(6, RoundingMode.HALF_UP);
        markovChainSource.updateScale(rounded);
        logger.info("Markov chain scale updated to {}.", rounded);
    }

    private LocalDateTime dateOffset() {
        return LocalDateTime.now().minusMonths(MONTH_OFFSET);
    }
}