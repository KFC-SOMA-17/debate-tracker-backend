package com.debatetracker.debate.infrastructure.persistence.redis.dto;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import java.math.BigDecimal;

public record RefinedSegmentJson(String id, String content, String speaker, BigDecimal startAt, BigDecimal endAt) {

    public RefinedSegmentJson(RefinedSpeechSegment segment) {
        this(
                segment.getId(),
                segment.getContent(),
                segment.getSpeaker(),
                segment.getStartAt(),
                segment.getEndAt()
        );
    }

    public RefinedSpeechSegment toDomain() {
        return new RefinedSpeechSegment(id, content, speaker, startAt, endAt);
    }
}
