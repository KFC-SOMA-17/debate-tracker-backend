package com.debatetracker.infra.llm.config;

import com.debatetracker.infra.llm.chat.refine.RefineLlmChat;
import com.debatetracker.infra.llm.chat.refine.RefineLlmSelector;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(
        afterName = "org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration")
@RequiredArgsConstructor
public class LlmConfiguration {

    private final ObjectMapper objectMapper;

    @Bean
    @ConditionalOnBean(ChatModel.class)
    public RefineLlmChat refineLlmChat(ChatModel chatModel,
                                       @Value("${llm.refine.user-prompt}") String userPrompt,
                                       @Value("${llm.refine.system-prompt}") String systemPrompt) {
        return new RefineLlmChat(new RefineLlmSelector(ChatClient.create(chatModel)),
                systemPrompt, userPrompt, objectMapper);
    }
}
