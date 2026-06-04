package com.debatetracker.debate.ws.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class WebSocketMessage {

    private final long debateId;
    private final MessageType type;

    @JsonProperty("debateId")
    public final long debateId() {
        return debateId;
    }

    @JsonProperty("type")
    public final MessageType type() {
        return type;
    }

    @JsonProperty("data")
    public abstract Object data();
}
