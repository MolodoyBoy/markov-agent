package com.markov.agent.domain.open_ai;

import com.markov.agent.domain.source.AIClient;
import com.markov.agent.domain.tool_callback.CompanyStockStatusToolCallback;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenAIClient implements AIClient {

    private final ChatClient chatClient;
    private final String systemMessageContent;
    private final CompanyStockStatusToolCallback companyStockStatusToolCallback;

    public OpenAIClient(ChatClient opeAIClient,
                        CompanyStockStatusToolCallback companyStockStatusToolCallback,
                        @Value("classpath:/prompts/system-prompt.st") String systemMessageContent) {
        this.chatClient = opeAIClient;
        this.systemMessageContent = systemMessageContent;
        this.companyStockStatusToolCallback = companyStockStatusToolCallback;
    }

    @Override
    public String getResponse(String request) {
        Prompt prompt = buildPrompt(request);

        return chatClient.prompt(prompt)
            .toolCallbacks(companyStockStatusToolCallback)
            .call()
            .content();
    }

    private Prompt buildPrompt(String request) {
        Message systemMessage = new SystemMessage(systemMessageContent);
        Message userMessage = new UserMessage(request);

        return new Prompt(List.of(systemMessage, userMessage));
    }
}