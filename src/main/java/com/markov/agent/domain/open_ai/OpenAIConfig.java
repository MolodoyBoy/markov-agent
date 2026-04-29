package com.markov.agent.domain.open_ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Bean
    public ChatClient opeAIClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}
