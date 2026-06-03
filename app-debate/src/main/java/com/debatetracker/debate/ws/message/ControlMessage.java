package com.debatetracker.debate.ws.message;

public record ControlMessage(
        ControlMessageType type,
        String sessionId
) {

}
