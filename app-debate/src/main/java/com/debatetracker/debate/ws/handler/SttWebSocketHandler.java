package com.debatetracker.debate.ws.handler;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.service.debate.DebateStreamingService;
import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.debate.ws.message.ControlMessage;
import com.debatetracker.serdes.SerDesUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * /ws/stt 핸들러. 브라우저가 보내는 START/STOP 제어(Text)와 PCM 오디오(Binary)를 처리한다.
 * 실제 STT 호출은 infra-stt 의 SttClient 에 위임하고, 전사 결과는 TranscribeEventListener 가 전송한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stt.azure.enabled", havingValue = "true")
public class SttWebSocketHandler extends AbstractWebSocketHandler {

    private final DebateStreamingService debateStreamingService;
    private final WebSocketMessageSender messageSender;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("STT WebSocket 연결: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        ControlMessage control = SerDesUtils.deserialize(message.getPayload(), ControlMessage.class);
        WebSocketMessage webSocketMessage = debateStreamingService.handleControlMessage(control, session);
        messageSender.send(new DebateSession(session),  webSocketMessage);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        Object debateId = session.getAttributes().get(DebateSession.ATTR_DEBATE_ID);
        if (debateId == null) {
            log.debug("START 이전 바이너리 수신, 무시: {}", session.getId());
            return;
        }
        debateStreamingService.sendAudioChunk(debateId.toString(), message.getPayload().array());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("STT WebSocket 종료: {} (status={})", session.getId(), status);
        debateStreamingService.stopDebateIfActive(session);
    }
}
