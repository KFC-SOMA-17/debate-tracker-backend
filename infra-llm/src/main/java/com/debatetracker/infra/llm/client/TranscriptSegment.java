package com.debatetracker.infra.llm.client;

import java.math.BigDecimal;

public record TranscriptSegment(
        String id,
        String speaker,
        BigDecimal start,
        BigDecimal end,
        String text
) {
}
