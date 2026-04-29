package com.markov.agent.application.args;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static java.util.function.Function.identity;
import static com.markov.agent.application.args.ArgumentParser.Status.*;

@Component
public class ArgumentParser {

    private static final String DELIMITER = "=";
    private final Map<String, ArgumentKey> argumentKeys = stream(ArgumentKey.values())
        .collect(toMap(ArgumentKey::key, identity()));

    public List<ParserResult> parser(String[] args) {
        if (args.length == 0) {
            return null;
        }

        List<ParserResult> results = new ArrayList<>(args.length);

        for (String arg : args) {
            String[] split = arg.split(DELIMITER);

            if (split.length != 2) {
                results.add(new ParserResult(null, arg, INVALID_FORMAT));
                continue;
            }

            ArgumentKey argumentKey = argumentKeys.get(split[0]);
            if (argumentKey == null) {
                results.add(new ParserResult(null, split[0], UNKNOWN_KEY));
                continue;
            }

            results.add(new ParserResult(argumentKey, split[1], SUCCESS));
        }

        return results;
    }

    public enum Status {
        SUCCESS,
        UNKNOWN_KEY,
        INVALID_FORMAT
    }

    public record ParserResult(ArgumentKey argumentKey, String value, Status status) {}
}
