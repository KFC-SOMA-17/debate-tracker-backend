package com.debatetracker.debate.ws.handler;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.debate.ws.message.ControlMessage;
import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.infra.stt.client.SttClient;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final String ATTR_DEBATE_ID = "debateId";

    private final SttClient sttClient;
    private final DebateSessionRepository sessionRepository;
    private final WebSocketMessageSender messageSender;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("STT WebSocket 연결: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ControlMessage control = objectMapper.readValue(message.getPayload(), ControlMessage.class);
        switch (control.type()) {
            case ControlMessage.TYPE_START -> startDebate(session, control.sessionId());
            case ControlMessage.TYPE_STOP -> stopDebate(control.sessionId());
            default -> log.warn("알 수 없는 제어 메시지 타입: {}", control.type());
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String debateId = (String) session.getAttributes().get(ATTR_DEBATE_ID);
        if (debateId == null) {
            log.debug("START 이전 바이너리 수신, 무시: {}", session.getId());
            return;
        }
        sttClient.sendAudioChunk(debateId, message.getPayload().array());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("STT WebSocket 종료: {} (status={})", session.getId(), status);
        String debateId = (String) session.getAttributes().get(ATTR_DEBATE_ID);
        if (debateId != null && sessionRepository.existsByDebateId(debateId)) {
            stopDebate(debateId);
        }
    }

    private void startDebate(WebSocketSession session, String debateId) {
        session.getAttributes().put(ATTR_DEBATE_ID, debateId);
        sessionRepository.save(new DebateSession(debateId, session));
        sttClient.startStreaming(debateId);
        messageSender.send(new DebateSession(debateId, session), WebSocketMessage.debateStart(Long.parseLong(debateId)));
        log.info("토론 시작: debateId={}", debateId);
    }

    private void stopDebate(String debateId) {
        sttClient.stopStreaming(debateId);
        sessionRepository.deleteByDebateId(debateId)
                .ifPresent(session -> messageSender.send(session, WebSocketMessage.debateEnd(Long.parseLong(debateId))));
        log.info("토론 종료: debateId={}", debateId);
    }
}
