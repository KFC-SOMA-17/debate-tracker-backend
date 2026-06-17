package com.debatetracker.infra.stt.service.azure;

import com.debatetracker.infra.stt.client.dto.SttSegment;
import com.debatetracker.infra.stt.client.event.TranscribeEvent;
import com.debatetracker.infra.stt.repository.AzureSessionRepository;
import com.debatetracker.infra.stt.session.AzureSession;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriptionCanceledEventArgs;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriptionEventArgs;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriptionResult;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@RequiredArgsConstructor
public class AzureSttService {

    private static final String VENDOR_NAME = "azure"; // TODO 추후 로깅 개선 예정

    private final AzureSessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void handleStartStreaming(String sessionId, AzureSession session) {
        sessionRepository.save(session);
        log.info("[{}] 전사 시작 성공, session={}", VENDOR_NAME, sessionId);
    }

    public void handleSendAudioChunk(String sessionId, byte[] pcmData) {
        sessionRepository.findBySessionId(sessionId)
                .ifPresentOrElse(
                        session -> {
                            try {
                                session.pushStream().write(pcmData);
                            } catch (Exception e) {
                                log.error("[{}] 오디오 전송 에러: session={}, error={}", VENDOR_NAME, sessionId, e.getMessage(), e);
                            }
                        },
                        () -> log.debug("[{}] 활성 세션 없음, 오디오 무시: {}", VENDOR_NAME, sessionId)
                );
    }

    public void handleStopStreaming(String sessionId) {
        log.info("[{}] stopStreaming called: {}", VENDOR_NAME, sessionId);
        sessionRepository.deleteBySessionId(sessionId)
                .ifPresent(AzureSession::close);
    }

    public boolean isSessionActive(String sessionId) {
        return sessionRepository.existsBySessionId(sessionId);
    }

    public void handleTranscribed(String sessionId, ConversationTranscriptionEventArgs e) {
        ConversationTranscriptionResult result = e.getResult();
        if (result.getReason() == ResultReason.RecognizedSpeech
                && result.getText() != null
                && !result.getText().isEmpty()
        ) {
            long offsetTicks = result.getOffset().longValue();
            long durationTicks = result.getDuration().longValue();
            SttSegment sttSegment = new SttSegment(
                    BigDecimal.valueOf(offsetTicks / 10_000_000.0),
                    BigDecimal.valueOf((offsetTicks + durationTicks) / 10_000_000.0),
                    result.getSpeakerId(),
                    result.getText()
            );
            eventPublisher.publishEvent(new TranscribeEvent(sessionId, sttSegment));
        }
    }

    public void handleSessionStarted(String sessionId) {
        log.info("[{}] 세션 시작: {}", VENDOR_NAME, sessionId);
    }

    public void handleSessionStopped(String sessionId) {
        log.info("[{}] 세션 종료: {}", VENDOR_NAME, sessionId);
        sessionRepository.deleteBySessionId(sessionId);
    }

    public void handleCanceled(String sessionId, ConversationTranscriptionCanceledEventArgs e) {
        log.error("[{}] 인식 취소: session={}, reason={}, errorCode={}, errorDetails={}",
                VENDOR_NAME, sessionId, e.getReason(), e.getErrorCode(), e.getErrorDetails());
    }
}
