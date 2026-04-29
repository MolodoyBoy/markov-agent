package com.markov.agent.data_loader.importer;

import com.markov.agent.data_loader.source.LockSource;
import com.markov.agent.domain.source.MarkovChainSource;
import com.markov.agent.domain.value_object.MathIndex;
import com.markov.agent.domain.value_object.State;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class MarkovChainImporter implements Importer {

    private static final Logger logger = getLogger(MarkovChainImporter.class);

    private static final int LOCK_KEY = 1;
    private static final int MONTH_OFFSET = 36;

    private final LockSource lockSource;
    private final MarkovChainSource markovChainSource;

    public MarkovChainImporter(LockSource lockSource,
                               MarkovChainSource markovChainSource) {
        this.lockSource = lockSource;
        this.markovChainSource = markovChainSource;
    }

    @Override
    public void startImport() {
        lockSource.acquireLock(LOCK_KEY);

        logger.info("Start marcov chain import.");

        LocalDateTime fromDate = dateOffset();

        for (MathIndex mathIndex : MathIndex.values()) {
            markovChainSource.updateMathIndex(fromDate, mathIndex);
        }

        for (State from : State.values()) {
            for (State to : State.values()) {
                markovChainSource.updateMarkovChain(from, to, fromDate);
            }
        }

        logger.info("Finish marcov chain import.");
    }

    private LocalDateTime dateOffset() {
        return LocalDateTime.now().minusMonths(MONTH_OFFSET);
    }
}