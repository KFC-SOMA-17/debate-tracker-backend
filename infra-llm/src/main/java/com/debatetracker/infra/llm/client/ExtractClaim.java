package com.debatetracker.infra.llm.client;

import java.util.List;
import org.springframework.lang.Nullable;

public record ExtractClaim(
        @Nullable String id,
        ExtractStance stance,
        String content,
        List<ExtractedEvidence> evidences) {
}
