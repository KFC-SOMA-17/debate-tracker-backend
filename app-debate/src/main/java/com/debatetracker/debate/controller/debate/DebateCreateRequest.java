package com.debatetracker.debate.controller.debate;

import com.debatetracker.debate.domain.debate.Debate;
import jakarta.validation.constraints.NotBlank;

public record DebateCreateRequest(
        @NotBlank String topic
) {

    public Debate toDomain() {
        return new Debate(null, topic);
    }
}
