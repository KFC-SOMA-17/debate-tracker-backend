package com.debatetracker.infra.llm.adapter;

import com.debatetracker.infra.llm.chat.refine.RefineLlmChat;
import com.debatetracker.infra.llm.client.LlmClient;
import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiLlmClient implements LlmClient {

    private final RefineLlmChat refineLlmChat;

    @Override
    public RefineResponse refine(RefineRequest request) {
        return refineLlmChat.fetch(request);
    }
}
