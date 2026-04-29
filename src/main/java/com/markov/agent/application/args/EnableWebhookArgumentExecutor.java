package com.markov.agent.application.args;

import com.markov.agent.application.event.SetWebhookEvent;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import static com.markov.agent.application.args.ArgumentKey.WEBHOOK;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class EnableWebhookArgumentExecutor implements ArgumentExecutor {

    private static final Logger logger = getLogger(EnableWebhookArgumentExecutor.class);

    private final ApplicationContext applicationContext;

    public EnableWebhookArgumentExecutor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void execute(String value) {
        boolean enabled = Boolean.parseBoolean(value);

        logger.info("Set webhook to: {}", enabled ? "enabled" : "disabled");
        if (enabled) {
            applicationContext.publishEvent(new SetWebhookEvent(applicationContext));
        }
    }

    @Override
    public ArgumentKey argumentKey() {
        return WEBHOOK;
    }
}
