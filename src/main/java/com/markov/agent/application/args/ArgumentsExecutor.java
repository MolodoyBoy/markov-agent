package com.markov.agent.application.args;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;
import static java.util.function.Function.identity;
import static com.markov.agent.application.args.ArgumentParser.ParserResult;

@Component
public class ArgumentsExecutor {

    private static final Logger logger = getLogger(ArgumentsExecutor.class);

    private final ArgumentParser argumentParser;
    private final Map<ArgumentKey, ArgumentExecutor> argumentExecutors;

    public ArgumentsExecutor(ArgumentParser argumentParser, List<ArgumentExecutor> argumentExecutors) {
        this.argumentParser = argumentParser;
        this.argumentExecutors = argumentExecutors.stream()
            .collect(Collectors.toMap(ArgumentExecutor::argumentKey, identity()));
    }

    public void execute(String[] args) {
        List<ParserResult> results = argumentParser.parser(args);
        if (results == null) {
            return;
        }

        for (ParserResult result : results) {
            int status = switch (result.status()) {
                case SUCCESS -> {
                    ArgumentExecutor executor = argumentExecutors.get(result.argumentKey());
                    if (executor == null) {
                        logger.warn("No executor for argument key: {}", result.argumentKey());
                        yield 0;
                    }

                    executor.execute(result.value());
                    yield 0;
                }

                case UNKNOWN_KEY -> {
                    logger.error("Unknown argument key: {}", result.value());
                    yield 1;
                }

                case INVALID_FORMAT -> {
                    logger.error("Invalid argument format: {}", result.value());
                    yield 1;
                }
            };

            if (status != 0) {
                System.exit(status);
            }
        }
    }
}
