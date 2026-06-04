package com.debatetracker.debate.ws.sender;

import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.serdes.SerDesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocketMessage 를 JSON 텍스트 프레임으로 직렬화해 클라이언트 연결로 전송한다.
 * 핸들러(DEBATE_START/END)와 전사 이벤트 리스너(TRANSCRIPTION)가 공유한다.
 */
@Slf4j
@Component
public class WebSocketMessageSender {

    public void send(DebateSession session, WebSocketMessage message) {
        send(session.connection(), message);
    }

    public void send(WebSocketSession connection, WebSocketMessage message) {
        try {
            String payload = SerDesUtils.serialize(message);
            connection.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패: debateId={}, type={}", message.debateId(), message.type(), e);
        }
    }
}
