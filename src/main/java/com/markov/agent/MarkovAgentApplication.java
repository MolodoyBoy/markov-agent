package com.markov.agent;

import com.markov.agent.application.args.ArgumentsExecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry
@EnableScheduling
@SpringBootApplication
public class MarkovAgentApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(MarkovAgentApplication.class, args);

        var argumentsExecutor = context.getBean(ArgumentsExecutor.class);
        argumentsExecutor.execute(args);
    }
}