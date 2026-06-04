package com.debatetracker.debate.infrastructure.persistence.redis.dto;

import com.debatetracker.debate.domain.transcript.SpeechSegment;
import java.math.BigDecimal;

public record RawSegmentJson(String id, String content, String speaker, BigDecimal startAt, BigDecimal endAt) {

    public RawSegmentJson(SpeechSegment segment) {
        this(
                segment.getId(),
                segment.getContent(),
                segment.getSpeaker(),
                segment.getStartAt(),
                segment.getEndAt()
        );
    }

    public SpeechSegment toDomain() {
        return new SpeechSegment(id, content, speaker, startAt, endAt);
    }
}
