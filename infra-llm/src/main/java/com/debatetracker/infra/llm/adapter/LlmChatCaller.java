package com.debatetracker.infra.llm.adapter;

/**
 * LLM 단발 호출 seam. 정제 로직(검증/재시도/폴백)을 transport(Spring AI) 와 분리해
 * 단위 테스트에서 Mockito 로 격리할 수 있게 한다.
 */
public interface LlmChatCaller {

    String call(String systemPrompt, String userPrompt);
}
