package com.debatetracker.debate.ws.message;

public record WebSocketMessage(
        long debateId,
        MessageType type,
        Object data
) {

    public static WebSocketMessage debateStart(long debateId) {
        return new WebSocketMessage(debateId, MessageType.DEBATE_START, null);
    }

    public static WebSocketMessage transcription(long debateId, TranscriptionSegment segment) {
        return new WebSocketMessage(debateId, MessageType.TRANSCRIPTION, segment);
    }

    public static WebSocketMessage debateEnd(long debateId) {
        return new WebSocketMessage(debateId, MessageType.DEBATE_END, null);
    }
}
