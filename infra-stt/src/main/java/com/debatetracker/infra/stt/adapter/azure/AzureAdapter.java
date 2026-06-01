package com.debatetracker.infra.stt.adapter.azure;

import com.debatetracker.infra.stt.client.SttClient;
import com.debatetracker.infra.stt.config.AudioProperties;
import com.debatetracker.infra.stt.config.AzureConfig;
import com.debatetracker.infra.stt.dto.SttSegment;
import com.debatetracker.infra.stt.dto.TranscriberSession;
import com.microsoft.cognitiveservices.speech.OutputFormat;
import com.microsoft.cognitiveservices.speech.ProfanityOption;
import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriber;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriptionResult;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Azure AI Speech STT 벤더 어댑터. ConversationTranscriber를 사용하여 화자분리 + 한국어 전사를 수행한다. 세션별로 독립된 연결을 관리하여 다중 세션 동시 처리를 지원한다.
 */
@Slf4j
public class AzureAdapter implements SttClient {

    private static final String VENDOR_NAME = "azure";

    private final AzureConfig config;
    private final AudioProperties audioProperties;
    private final ConcurrentHashMap<String, TranscriberSession> sessions = new ConcurrentHashMap<>();

    public AzureAdapter(AzureConfig azureConfig, AudioProperties audioProperties) {
        if (azureConfig == null || !azureConfig.enabled()) {
            throw new RuntimeException("Azure configuration is not set"); //TODO DebateTrackerException으로 변경 예정
        }
        this.config = azureConfig;
        this.audioProperties = audioProperties;
    }

    @Override
    public String getVendorName() {
        return VENDOR_NAME;
    }

    @Override
    public void startStreaming(String sessionId, Consumer<SttSegment> onSegment) {
        //TODO 따닥 문제 추후 고려
        if (sessions.containsKey(sessionId)) {
            log.warn("[{}] 이미 활성 세션이 존재합니다: {}", VENDOR_NAME, sessionId);
            return;
        }
        connect(sessionId, onSegment);
    }

    private void connect(String sessionId, Consumer<SttSegment> onSegment) {
        try {
            SpeechConfig speechConfig = buildSpeechConfig();
            AudioStreamFormat format = AudioStreamFormat.getWaveFormatPCM(audioProperties.sampleRate(), (short) 16, (short) 1);
            PushAudioInputStream pushStream = AudioInputStream.createPushStream(format);
            AudioConfig audioConfig = AudioConfig.fromStreamInput(pushStream);
            ConversationTranscriber transcriber = new ConversationTranscriber(speechConfig, audioConfig);
            TranscriberSession session = new TranscriberSession(
                    sessionId,
                    transcriber,
                    pushStream,
                    audioConfig,
                    speechConfig,
                    onSegment
            );

            transcriber.transcribed.addEventListener((s, e) -> {
                ConversationTranscriptionResult result = e.getResult();
                if (result.getReason() == ResultReason.RecognizedSpeech
                        && result.getText() != null
                        && !result.getText().isEmpty()
                ) {
                    long offsetTicks = result.getOffset().longValue();
                    long durationTicks = result.getDuration().longValue();
                    SttSegment sttSegment = new SttSegment(
                            BigDecimal.valueOf(offsetTicks / 10_000_000.0),
                            BigDecimal.valueOf(((offsetTicks + durationTicks) / 10_000_000.0)),
                            result.getSpeakerId(),
                            result.getText()
                    );
                    onSegment.accept(sttSegment);
                }
            });

            transcriber.sessionStarted.addEventListener((s, e) ->
                    log.info("[{}] 세션 시작: {}", VENDOR_NAME, sessionId));

            transcriber.sessionStopped.addEventListener((s, e) -> {
                log.info("[{}] 세션 종료: {}", VENDOR_NAME, sessionId);
                sessions.remove(sessionId);
            });

            transcriber.canceled.addEventListener((s, e) ->
                    log.error("[{}] 인식 취소: session={}, reason={}, errorCode={}, errorDetails={}",
                            VENDOR_NAME, sessionId, e.getReason(), e.getErrorCode(), e.getErrorDetails()));

            transcriber.startTranscribingAsync().get();
            sessions.put(sessionId, session);
            log.info("[{}] 전사 시작 성공, session={}", VENDOR_NAME, sessionId);

        } catch (Exception e) {
            log.error("[{}] 연결 실패: session={}, error={}", VENDOR_NAME, sessionId, e.getMessage(), e);
            throw new RuntimeException("Streaming Connection Failed"); //DebateTrackerException으로 변경 예정
        }
    }

    @Override
    public void sendAudioChunk(String sessionId, byte[] pcmData) {
        TranscriberSession session = sessions.get(sessionId);
        if (session == null) {
            log.debug("[{}] 활성 세션 없음, 오디오 무시: {}", VENDOR_NAME, sessionId);
            return;
        }
        try {
            session.pushStream().write(pcmData);
        } catch (Exception e) {
            log.error("[{}] 오디오 전송 에러: session={}, error={}", VENDOR_NAME, sessionId, e.getMessage(), e);
        }
    }

    @Override
    public void stopStreaming(String sessionId) {
        log.info("[{}] stopStreaming called: {}", VENDOR_NAME, sessionId);
        TranscriberSession session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        session.close();
    }

    @Override
    public boolean isConnected(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    private SpeechConfig buildSpeechConfig() {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(config.subscriptionKey(), config.region());
        speechConfig.setSpeechRecognitionLanguage(config.language());
        speechConfig.setProfanity(ProfanityOption.Raw);
        speechConfig.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs,
                String.valueOf(config.silenceTimeoutMs()));
        speechConfig.enableDictation();
        speechConfig.setOutputFormat(OutputFormat.Detailed);
        return speechConfig;
    }
}
