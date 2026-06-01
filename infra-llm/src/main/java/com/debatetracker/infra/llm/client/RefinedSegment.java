package com.debatetracker.infra.llm.client;

/**
 * LLM 응답 JSON 한 항목의 파싱 대상. text(+speaker)만 채택하며 start/end/id 골격은 원본에서 복사한다.
 */
public record RefinedSegment(String id, String speaker, String text) {
}
