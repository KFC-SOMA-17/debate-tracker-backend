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

    //TODO 어디까지 실패하냐에 따라 각 롤백전략 분기 필요
    public boolean stopDebateWithRemainingRefine(String debateId) {
        if (debateId == null || !sessionRepository.existsByDebateId(debateId)) {
            return false;
        }
        sttClient.stopStreaming(debateId);
        sessionRepository.deleteByDebateId(debateId);
        boolean refined = refiningService.refineRemaining(new DebateSession(debateId));
        if (refined) {
            bufferRepository.clear(debateId);
            log.info("STOP 으로 토론 정리(남은 raw 보정 후): debateId={}", debateId);
            return true;
        }
        log.warn("STOP 시 남은 raw 보정 실패 — buffer 를 유지한다: debateId={}", debateId);
        return true;
    }

    public void startDebate(String debateId) {
        try {
            sessionRepository.save(new DebateSession(debateId));
            sttClient.startStreaming(debateId);
            log.info("토론 시작: debateId={}", debateId);
        } catch (Exception exception) {
            sessionRepository.deleteByDebateId(debateId);
            throw exception;
        }
    }


    public void sendAudioChunk(String debateId, byte[] payload) {
        sttClient.sendAudioChunk(debateId, payload);
    }

    public List<DebateSession> findActiveSessions() {
        return sessionRepository.findAll();
    }
}
