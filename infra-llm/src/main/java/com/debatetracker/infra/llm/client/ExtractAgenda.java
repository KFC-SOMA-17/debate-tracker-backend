package com.debatetracker.infra.llm.client;

import java.util.List;
import org.springframework.lang.Nullable;


public record ExtractAgenda(
        @Nullable String id,
        String content,
        List<ExtractClaim> claims) {
}
