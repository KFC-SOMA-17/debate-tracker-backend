package com.debatetracker.infra.llm.client;

import java.util.List;

public record RefineResponse(
        List<TranscriptSegment> segments
) {

    public RefineResponse {
        segments = List.copyOf(segments);
    }
}
