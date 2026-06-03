package com.debatetracker.debate.ws.message;

public record ControlMessage(
        ControlMessageType type,
        String sessionId
) {

    public boolean isStart() {
        return type.isStart();
    }

    public boolean isStop() {
        return type.isStop();
    }
}
