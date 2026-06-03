package com.debatetracker.infra.stt.client.event;

import com.debatetracker.infra.stt.client.dto.SttSegment;

public record TranscribeEvent(
        String sessionId,
        SttSegment segment
) {

}
