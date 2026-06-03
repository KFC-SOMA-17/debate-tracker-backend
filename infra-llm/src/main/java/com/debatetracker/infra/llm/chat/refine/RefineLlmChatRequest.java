package com.debatetracker.infra.llm.chat.refine;

import com.debatetracker.infra.llm.client.TranscriptSegment;
import java.util.List;

public record RefineLlmChatRequest(List<RefineLlmChatSegment> segments) {

    public static RefineLlmChatRequest from(List<TranscriptSegment> transcripts) {
        List<RefineLlmChatSegment> segments = transcripts.stream()
                .map(RefineLlmChatSegment::new)
                .toList();
        return new RefineLlmChatRequest(segments);
    }
}
