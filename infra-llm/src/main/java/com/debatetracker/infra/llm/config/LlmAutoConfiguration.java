package com.debatetracker.infra.llm.config;

import com.debatetracker.infra.llm.adapter.LlmChatCaller;
import com.debatetracker.infra.llm.adapter.SpringAiChatCaller;
import com.debatetracker.infra.llm.adapter.SpringAiLlmClient;
import com.debatetracker.infra.llm.client.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * infra-llm 자동설정. Spring AI {@code ChatModel} 빈이 존재할 때만 {@link LlmClient} 를 노출한다.
 *
 * <p>활성화 스위치는 Spring AI 의 provider 선택자({@code spring.ai.model.chat=google-genai}) + API Key 다.
 * provider 가 꺼져 있으면(기본 {@code none}) ChatModel 이 없어 이 빈도 생성되지 않는다 — 키 없이도 앱이 부팅된다.
 * 런타임 mock 빈은 제공하지 않는다 — 테스트 격리는 Mockito / {@code @MockBean} 으로 한다.
 */
@AutoConfiguration(
        afterName = "org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration")
public class LlmAutoConfiguration {

    private static final int REFINE_POOL_SIZE = 4;

    @Bean
    @ConditionalOnBean(ChatModel.class)
    public LlmClient llmClient(ChatModel chatModel, ObjectMapper objectMapper) {
        LlmChatCaller caller = new SpringAiChatCaller(ChatClient.create(chatModel));
        Executor executor = Executors.newFixedThreadPool(REFINE_POOL_SIZE);
        return new SpringAiLlmClient(caller, objectMapper, executor);
    }
}
