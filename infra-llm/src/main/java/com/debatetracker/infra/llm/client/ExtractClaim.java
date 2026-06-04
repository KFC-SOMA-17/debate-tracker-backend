package com.debatetracker.infra.llm.client;

import java.util.List;

public record ExtractClaim(ExtractStance stance, String content, List<ExtractedEvidence> evidences) {
}
