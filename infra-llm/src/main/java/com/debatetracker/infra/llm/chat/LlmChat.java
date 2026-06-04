package com.debatetracker.infra.llm.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;

@RequiredArgsConstructor
public abstract class LlmChat<REQ, RES> {

    private final LlmSelector selector;
    private final String systemPrompt;
    private final String userPrompt;

    public final RES fetch(REQ request) {
        ChatClient chatClient = selector.select();
        String rawResponse = chatClient.prompt()
                .system(processSystemPrompt(request))
                .user(processUserPrompt(request))
                .call()
                .content();

        String stripResponse = stripCodeFence(rawResponse);
        RES response = refineResponse(request, stripResponse);
        validate(request, response);
        return response;
    }

    private String stripCodeFence(String raw) {
        String trimmed = raw.strip();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline < 0) {
            return trimmed;
        }
        String withoutOpening = trimmed.substring(firstNewline + 1);
        int closingFence = withoutOpening.lastIndexOf("```");
        if (closingFence < 0) {
            return withoutOpening.strip();
        }
        return withoutOpening.substring(0, closingFence).strip();
    }

    protected abstract String processSystemPrompt(REQ request);

    protected abstract String processUserPrompt(REQ request);

    protected abstract RES refineResponse(REQ request, String rawResponse);

    protected abstract void validate(REQ request, RES response);

    protected final String getUserPrompt() {
        return userPrompt;
    }

    protected final String getSystemPrompt() {
        return systemPrompt;
    }
}
