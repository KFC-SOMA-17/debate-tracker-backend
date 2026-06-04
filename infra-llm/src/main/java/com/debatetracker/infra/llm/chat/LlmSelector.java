package com.debatetracker.infra.llm.chat;

import org.springframework.ai.chat.client.ChatClient;

public interface LlmSelector {

    ChatClient select();
}
