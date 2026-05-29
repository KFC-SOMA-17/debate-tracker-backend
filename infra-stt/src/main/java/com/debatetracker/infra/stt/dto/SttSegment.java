package com.debatetracker.infra.stt.dto;

import java.math.BigDecimal;

public record SttSegment(
        BigDecimal start,
        BigDecimal end,
        String speaker,
        String content
) {

}
