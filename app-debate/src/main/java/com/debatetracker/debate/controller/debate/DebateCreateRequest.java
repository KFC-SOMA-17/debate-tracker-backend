package com.debatetracker.debate.controller.debate;

import com.debatetracker.debate.domain.debate.Debate;
import jakarta.validation.constraints.NotNull;

public record DebateCreateRequest(
        @NotNull String topic
) {

    public Debate toDomain() {
        return new Debate(null, topic);
    }
}
