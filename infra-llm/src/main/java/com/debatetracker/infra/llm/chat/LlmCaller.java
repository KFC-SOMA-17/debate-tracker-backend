package com.debatetracker.infra.llm.chat;

import java.util.function.Function;

public interface LlmCaller {

    <T> T call(String systemPrompt, String userPrompt, Function<String, T> responseProcessor);
}
