package com.debatetracker.debate.ws.message;

public class DebateEndMessage extends WebSocketMessage {

    public DebateEndMessage(long debateId) {
        super(debateId, MessageType.DEBATE_END);
    }

    @Override
    public Object data() {
        return null;
    }
}
