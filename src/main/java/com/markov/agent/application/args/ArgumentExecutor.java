package com.markov.agent.application.args;

public interface ArgumentExecutor {

    void execute(String value);

    ArgumentKey argumentKey();
}
