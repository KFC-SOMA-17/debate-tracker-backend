package com.debatetracker.infra.llm.client;

import java.util.List;
import java.util.Optional;

public record RefineRequest(
        String sessionId,
        String topic,
        List<TranscriptSegment> contexts,
        List<TranscriptSegment> targets
) {

    public RefineRequest {
        targets = List.copyOf(targets);
    }

    public Optional<TranscriptSegment> findTargetSegment(String id) {
        return targets.stream()
                .filter(segment -> segment.id().equals(id))
                .findFirst();
    }
}
