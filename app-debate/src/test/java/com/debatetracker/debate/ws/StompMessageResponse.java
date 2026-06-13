package com.debatetracker.debate.ws;

import com.debatetracker.debate.ws.message.MessageType;

public record StompMessageResponse(
        long debateId,
        MessageType type,
        Object data
) {
}
