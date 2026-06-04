package com.debatetracker.infra.llm.chat.extract;

import com.debatetracker.infra.llm.chat.LlmSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;

@RequiredArgsConstructor
public class ExtractLlmSelector implements LlmSelector {

    private final ChatClient chatClient;

    @Override
    public ChatClient select() {
        return chatClient;
    }
}
