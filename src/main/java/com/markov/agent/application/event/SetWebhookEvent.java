package com.markov.agent.application.event;

import org.springframework.context.ApplicationEvent;

public class SetWebhookEvent extends ApplicationEvent {

    public SetWebhookEvent(Object source) {
        super(source);
    }
}