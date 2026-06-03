package com.debatetracker.debate.controller.debate;

import com.debatetracker.debate.domain.debate.Debate;

public record DebateCreateResponse(String debateId, String topic) {

    public DebateCreateResponse(Debate debate) {
        this(Long.toString(debate.getId()), debate.getTopic());
    }
}
