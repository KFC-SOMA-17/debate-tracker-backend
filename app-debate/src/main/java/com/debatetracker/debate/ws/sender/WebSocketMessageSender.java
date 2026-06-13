package com.debatetracker.debate.ws.sender;

import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.serdes.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketMessageSender {

    private static final String TOPIC_DESTINATION_FORMAT = "/topic/debate/%s";

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(String debateId, WebSocketMessage message) {
        messagingTemplate.convertAndSend(TOPIC_DESTINATION_FORMAT.formatted(debateId), message);
    }

    public void send(WebSocketSession connection, WebSocketMessage message) {
        try {
            String payload = JsonUtils.serialize(message);
            connection.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패: debateId={}, type={}", message.debateId(), message.type(), e);
        }
    }
}
