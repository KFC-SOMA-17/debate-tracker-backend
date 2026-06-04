package com.debatetracker.infra.llm.client;

import java.util.List;

public record ExtractAgenda(String content, List<ExtractClaim> evidences) {
}
