package com.debatetracker.infra.llm.adapter;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Spring AI {@link ChatClient} 기반 transport 구현. 벤더 SDK 타입은 여기서도 보지 않으며
 * Spring AI 가 흡수한다. 벤더 교체는 starter + properties 변경만으로 가능.
 */
public class SpringAiChatCaller implements LlmChatCaller {

    private final ChatClient chatClient;

    public SpringAiChatCaller(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String call(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }
}
