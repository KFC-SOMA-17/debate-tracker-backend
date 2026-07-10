package com.debatetracker.infra.llm.config;

import com.debatetracker.infra.llm.adapter.SpringAiLlmClient;
import com.debatetracker.infra.llm.chat.ChatClientCaller;
import com.debatetracker.infra.llm.chat.extract.ExtractLlmChat;
import com.debatetracker.infra.llm.chat.refine.RefineLlmChat;
import com.debatetracker.infra.llm.client.LlmClient;
import com.debatetracker.infra.llm.log.LlmChatLogger;
import com.debatetracker.infra.llm.log.LlmOperationType;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@AutoConfiguration(
        afterName = "org.springframework.ai.model.bedrock.converse.autoconfigure.BedrockConverseProxyChatAutoConfiguration")
@ConditionalOnProperty(name = "llm.enabled", havingValue = "true")
public class LlmAutoConfiguration {

    @Bean
    public LlmChatLogger llmChatLogger(MeterRegistry meterRegistry) {
        return new LlmChatLogger(meterRegistry);
    }

    @Bean
    public RefineLlmChat refineLlmChat(ChatModel chatModel,
                                       @Value("${llm.refine.model}") String model,
                                       @Value("classpath:prompts/refine-system.txt") Resource systemPrompt,
                                       @Value("classpath:prompts/refine-user.txt") Resource userPrompt,
                                       LlmChatLogger logger) {
        ChatClientCaller caller = new ChatClientCaller(createChatClient(chatModel, model), logger, LlmOperationType.REFINE);
        return new RefineLlmChat(caller, readPrompt(systemPrompt), readPrompt(userPrompt));
    }

    @Bean
    public ExtractLlmChat extractLlmChat(ChatModel chatModel,
                                         @Value("${llm.extract.model}") String model,
                                         @Value("classpath:prompts/extract-system.txt") Resource systemPrompt,
                                         @Value("classpath:prompts/extract-user.txt") Resource userPrompt,
                                         LlmChatLogger logger) {
        ChatClientCaller caller = new ChatClientCaller(createChatClient(chatModel, model), logger, LlmOperationType.EXTRACT);
        return new ExtractLlmChat(caller, readPrompt(systemPrompt), readPrompt(userPrompt));
    }

    private ChatClient createChatClient(ChatModel chatModel, String model) {
        return ChatClient.builder(chatModel)
                .defaultOptions(ChatOptions.builder().model(model).build())
                .build();
    }

    @Bean
    public LlmClient llmClient(RefineLlmChat refineLlmChat, ExtractLlmChat extractLlmChat) {
        return new SpringAiLlmClient(refineLlmChat, extractLlmChat);
    }

    private String readPrompt(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("프롬프트 리소스를 읽지 못했습니다: " + resource, exception);
        }
    }
}
