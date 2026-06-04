package com.debatetracker.infra.llm.chat.refine;

import com.debatetracker.infra.llm.client.TranscriptSegment;
import java.math.BigDecimal;

public record RefineLlmChatSegment(String id, String speaker, String text) {

    public RefineLlmChatSegment(TranscriptSegment segment) {
        this(segment.id(), segment.speaker(), segment.text());
    }

    public TranscriptSegment toTranscriptSegment(BigDecimal start, BigDecimal end) {
        return new TranscriptSegment(id, speaker, start, end, text);
    }
}
