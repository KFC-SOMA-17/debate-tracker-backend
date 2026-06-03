package com.debatetracker.infra.llm.chat.refine;

import java.util.List;

public record RefineLlmChatResponse(List<RefineLlmChatSegment> segments) {
}
