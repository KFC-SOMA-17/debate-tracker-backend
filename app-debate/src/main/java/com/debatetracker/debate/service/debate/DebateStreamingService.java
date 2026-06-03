package com.debatetracker.debate.service.debate;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.debate.ws.message.ControlMessage;
import com.debatetracker.debate.ws.message.DebateEndMessage;
import com.debatetracker.debate.ws.message.DebateStartMessage;
import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.infra.stt.client.SttClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebateStreamingService {

    private static final String ATTR_DEBATE_ID = "debateId";

    private final SttClient sttClient;
    private final DebateSessionRepository sessionRepository;

    public WebSocketMessage handleControlMessage(ControlMessage message, WebSocketSession session) {
        if(message.isStart()) {
            return startDebate(session, message.sessionId());
        }
        return stopDebate(session.getId());
    }

    private WebSocketMessage startDebate(WebSocketSession session, String sessionId) {
        session.getAttributes().put(ATTR_DEBATE_ID, sessionId);
        sessionRepository.save(new DebateSession(sessionId, session));
        sttClient.startStreaming(sessionId);
        log.info("토론 시작: debateId={}", sessionId);
        return new DebateStartMessage(Long.parseLong(sessionId));
    }

    public WebSocketMessage stopDebate(String debateId) {
        if(debateId == null || !sessionRepository.existsByDebateId(debateId)) {
            throw new RuntimeException("토론 세션이 존재하지 않습니다");
        }
        sttClient.stopStreaming(debateId);
        sessionRepository.deleteByDebateId(debateId);
        log.info("토론 종료: debateId={}", debateId);
        return new DebateEndMessage(Long.parseLong(debateId));
    }

    public void sendAudioChunk(String sessionId, byte [] payload) {
        sttClient.sendAudioChunk(String.valueOf(sessionId), payload);
    }
}
