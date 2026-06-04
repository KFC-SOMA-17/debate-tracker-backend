package com.debatetracker.infra.llm.client;

import org.springframework.lang.Nullable;

public record ExtractedEvidence(
        @Nullable String id,
        ExtractEvidenceType type,
        String content) {
}
