package com.debatetracker.infra.stt.router;

import com.debatetracker.infra.stt.client.SttClient;
import com.debatetracker.infra.stt.logger.SttAudioChunkStatus;
import com.debatetracker.infra.stt.logger.SttLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SttClientRouter implements SttClient {

    private final SttSessionCreator sessionCreator;
    private final SttSessionRepository sessionRepository;
    private final SttLogger sttLogger;

    @Override
    public void startStreaming(String sessionId) {
        if (sessionRepository.existsBySessionId(sessionId)) {
            log.warn("이미 활성 세션이 존재합니다: {}", sessionId);
            return;
        }
        SttSession session = sessionCreator.create(sessionId);
        sessionRepository.save(sessionId, session);
    }

    @Override
    public void sendAudioChunk(String sessionId, byte[] pcmData) {
        sessionRepository.findBySessionId(sessionId)
                .ifPresentOrElse(
                        session -> {
                            session.sendAudio(pcmData);
                            sttLogger.recordAudioChunkSent(session.getVendor(), SttAudioChunkStatus.SUCCESS);
                        },
                        () -> log.debug("활성 세션 없음, 오디오 무시: {}", sessionId)
                );
    }

    @Override
    public void stopStreaming(String sessionId) {
        sessionRepository.deleteBySessionId(sessionId)
                .ifPresent(SttSession::stop);
    }

    @Override
    public boolean isConnected(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .map(SttSession::isConnected)
                .orElse(false);
    }
}
