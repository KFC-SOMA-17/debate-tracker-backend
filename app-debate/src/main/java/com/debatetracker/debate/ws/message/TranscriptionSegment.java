package com.debatetracker.debate.ws.message;

import java.math.BigDecimal;

public record TranscriptionSegment(
        String id,
        String content,
        String speaker,
        BigDecimal startAt,
        BigDecimal endAt
) {

}
