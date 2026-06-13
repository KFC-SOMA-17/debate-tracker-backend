package com.debatetracker.debate.service.debate;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.debate.domain.transcript.repository.TranscriptBufferRepository;
import com.debatetracker.debate.service.transcript.TranscribeRefiningService;
import com.debatetracker.infra.stt.client.SttClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebateStreamingService {

    private final SttClient sttClient;
    private final DebateSessionRepository sessionRepository;
    private final TranscriptBufferRepository bufferRepository;
    private final TranscribeRefiningService refiningService;

    public void stopDebateWithRemainingRefine(String debateId) {
        if (debateId == null || !sessionRepository.existsByDebateId(debateId)) {
            return;
        }
        sttClient.stopStreaming(debateId);
        sessionRepository.deleteByDebateId(debateId);
        refiningService.refineRemaining(new DebateSession(debateId));
        bufferRepository.clear(debateId);
        log.info("STOP 으로 토론 정리(남은 raw 보정 후): debateId={}", debateId);
    }

    public void stopDebateIfActive(String debateId) {
        if (debateId == null || !sessionRepository.existsByDebateId(debateId)) {
            return;
        }
        sttClient.stopStreaming(debateId);
        sessionRepository.deleteByDebateId(debateId);
        bufferRepository.clear(debateId);
        log.info("연결 종료로 토론 정리: debateId={}", debateId);
    }

    public void startDebate(String debateId) {
        sessionRepository.save(new DebateSession(debateId));
        sttClient.startStreaming(debateId);
        log.info("토론 시작: debateId={}", debateId);
    }


    public void sendAudioChunk(String debateId, byte[] payload) {
        sttClient.sendAudioChunk(debateId, payload);
    }

    public List<DebateSession> findActiveSessions() {
        return sessionRepository.findAll();
    }
}
