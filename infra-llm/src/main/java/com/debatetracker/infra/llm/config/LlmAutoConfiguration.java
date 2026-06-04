package com.debatetracker.infra.llm.config;

import com.debatetracker.infra.llm.adapter.SpringAiLlmClient;
import com.debatetracker.infra.llm.chat.refine.RefineLlmChat;
import com.debatetracker.infra.llm.chat.refine.RefineLlmSelector;
import com.debatetracker.infra.llm.client.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@AutoConfiguration(
        afterName = "org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration")
@ConditionalOnProperty(name = "llm.enabled", havingValue = "true")
@RequiredArgsConstructor
public class LlmAutoConfiguration {

    private final ObjectMapper objectMapper;

    @Bean
    public RefineLlmChat refineLlmChat(ChatModel chatModel,
                                       @Value("classpath:prompts/refine-system.txt") Resource systemPrompt,
                                       @Value("classpath:prompts/refine-user.txt") Resource userPrompt) {
        return new RefineLlmChat(new RefineLlmSelector(ChatClient.create(chatModel)),
                readPrompt(systemPrompt), readPrompt(userPrompt), objectMapper);
    }

    @Bean
    public LlmClient llmClient(RefineLlmChat refineLlmChat) {
        return new SpringAiLlmClient(refineLlmChat);
    }

    private String readPrompt(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("프롬프트 리소스를 읽지 못했습니다: " + resource, exception);
        }
    }
}
