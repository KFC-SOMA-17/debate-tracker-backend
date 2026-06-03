package com.debatetracker.debate.ws.message;

public class DebateStartMessage extends WebSocketMessage {

    public DebateStartMessage(long debateId) {
        super(debateId, MessageType.DEBATE_START);
    }

    @Override
    public Object data() {
        return null;
    }
}
