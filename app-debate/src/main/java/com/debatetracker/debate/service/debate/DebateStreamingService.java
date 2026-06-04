package com.debatetracker.debate.service.debate;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.debate.domain.transcript.repository.TranscriptBufferRepository;
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

    private final SttClient sttClient;
    private final DebateSessionRepository sessionRepository;
    private final TranscriptBufferRepository bufferRepository;

    public WebSocketMessage handleControlMessage(ControlMessage message, WebSocketSession session) {
        if (message.isStart()) {
            startDebate(session, message.sessionId());
            return new DebateStartMessage(Long.parseLong(message.sessionId()));
        }
        stopDebateIfActive(session);
        return new DebateEndMessage(Long.parseLong(message.sessionId()));
    }

    public void stopDebateIfActive(WebSocketSession session) {
        Object debateId = session.getAttributes().get(DebateSession.ATTR_DEBATE_ID);
        if (debateId == null || !sessionRepository.existsByDebateId(debateId.toString())) {
            return;
        }
        sttClient.stopStreaming(debateId.toString());
        sessionRepository.deleteByDebateId(debateId.toString());
        bufferRepository.clear(debateId.toString());
        log.info("연결 종료로 토론 정리: debateId={}", debateId);
    }

    private void startDebate(WebSocketSession session, String sessionId) {
        session.getAttributes().put(DebateSession.ATTR_DEBATE_ID, sessionId);
        sessionRepository.save(new DebateSession(sessionId, session));
        sttClient.startStreaming(sessionId);
        log.info("토론 시작: debateId={}", sessionId);
    }


    public void sendAudioChunk(String sessionId, byte[] payload) {
        sttClient.sendAudioChunk(String.valueOf(sessionId), payload);
    }
}
