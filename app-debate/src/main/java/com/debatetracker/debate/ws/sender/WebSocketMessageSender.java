package com.debatetracker.debate.ws.sender;

import com.debatetracker.debate.ws.message.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketMessageSender {

    private static final String TOPIC_DESTINATION_FORMAT = "/topic/debate/%s";

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(String debateId, WebSocketMessage message) {
        messagingTemplate.convertAndSend(TOPIC_DESTINATION_FORMAT.formatted(debateId), message);
    }
}
