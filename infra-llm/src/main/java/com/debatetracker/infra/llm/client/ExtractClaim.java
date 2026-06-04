package com.debatetracker.infra.llm.client;

import java.util.List;

public record ExtractClaim(String content, String stance, List<ExtractedEvidence> evidences) {
}
