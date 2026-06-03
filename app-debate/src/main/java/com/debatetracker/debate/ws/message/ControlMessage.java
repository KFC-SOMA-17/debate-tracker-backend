package com.debatetracker.debate.ws.message;

public record ControlMessage(
        String type,
        String sessionId
) {

    public static final String TYPE_START = "START";
    public static final String TYPE_STOP = "STOP";
}
