package com.debatetracker.infra.stt.session.azure;

import com.debatetracker.infra.stt.client.dto.SttSegment;
import com.debatetracker.infra.stt.client.event.TranscribeEvent;
import com.debatetracker.infra.stt.router.SttSession;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SessionEventArgs;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriber;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriptionCanceledEventArgs;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriptionEventArgs;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriptionResult;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
public class AzureSttSession implements SttSession {

    private static final String VENDOR_NAME = "azure";

    private final String sessionId;
    private final ConversationTranscriber transcriber;
    private final PushAudioInputStream pushStream;
    private final AudioConfig audioConfig;
    private final SpeechConfig speechConfig;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean connected = new AtomicBoolean(true);

    public AzureSttSession(String sessionId,
        ConversationTranscriber transcriber,
        PushAudioInputStream pushStream,
        AudioConfig audioConfig,
        SpeechConfig speechConfig,
        ApplicationEventPublisher eventPublisher) {
        this.sessionId = sessionId;
        this.transcriber = transcriber;
        this.pushStream = pushStream;
        this.audioConfig = audioConfig;
        this.speechConfig = speechConfig;
        this.eventPublisher = eventPublisher;
        registerEventListeners();
    }

    private void registerEventListeners() {
        transcriber.transcribed.addEventListener((s, e) -> handleTranscribed(e));
        transcriber.sessionStarted.addEventListener((s, e) -> handleSessionStarted(e));
        transcriber.sessionStopped.addEventListener((s, e) -> handleSessionStopped(e));
        transcriber.canceled.addEventListener((s, e) -> handleCanceled(e));
    }

    private void handleTranscribed(ConversationTranscriptionEventArgs e) {
        ConversationTranscriptionResult result = e.getResult();
        if (result.getReason() == ResultReason.RecognizedSpeech
            && result.getText() != null
            && !result.getText().isEmpty()) {
            long offsetTicks = result.getOffset().longValue();
            long durationTicks = result.getDuration().longValue();
            SttSegment segment = new SttSegment(
                BigDecimal.valueOf(offsetTicks / 10_000_000.0),
                BigDecimal.valueOf((offsetTicks + durationTicks) / 10_000_000.0),
                result.getSpeakerId(),
                result.getText()
            );
            eventPublisher.publishEvent(new TranscribeEvent(sessionId, segment));
        }
    }

    private void handleSessionStarted(SessionEventArgs e) {
        log.info("[{}] 세션 시작: {}", VENDOR_NAME, sessionId);
    }

    private void handleSessionStopped(SessionEventArgs e) {
        log.info("[{}] 세션 종료: {}", VENDOR_NAME, sessionId);
        connected.set(false);
    }

    private void handleCanceled(ConversationTranscriptionCanceledEventArgs e) {
        log.error("[{}] 인식 취소: session={}, reason={}, errorCode={}, errorDetails={}",
            VENDOR_NAME, sessionId, e.getReason(), e.getErrorCode(), e.getErrorDetails());
        connected.set(false);
    }

    @Override
    public void sendAudio(byte[] pcmData) {
        try {
            pushStream.write(pcmData);
        } catch (Exception e) {
            log.error("[{}] 오디오 전송 에러: session={}, error={}", VENDOR_NAME, sessionId,
                e.getMessage(), e); // TODO 재시도 로직 등 논의
        }
    }

    @Override
    public void stop() {
        AzureUtils.close(pushStream);
        AzureUtils.close(transcriber);
        AzureUtils.close(audioConfig);
        AzureUtils.close(speechConfig);
        connected.set(false);
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }
}
