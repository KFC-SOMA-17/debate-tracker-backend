package com.debatetracker.debate.ws.sender;

import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.serdes.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
public class WebSocketMessageSender {

    public void send(DebateSession session, WebSocketMessage message) {
        send(session.connection(), message);
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
