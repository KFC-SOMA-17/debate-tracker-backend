package com.debatetracker.infra.stt.adapter.azure;

import com.debatetracker.infra.stt.client.SttClient;
import com.debatetracker.infra.stt.config.AudioProperties;
import com.debatetracker.infra.stt.config.AzureConfig;
import com.debatetracker.infra.stt.service.azure.AzureSttService;
import com.debatetracker.infra.stt.session.AzureSession;
import com.microsoft.cognitiveservices.speech.OutputFormat;
import com.microsoft.cognitiveservices.speech.ProfanityOption;
import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriber;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AzureAdapter implements SttClient {

    private static final String VENDOR_NAME = "azure";

    private final AzureConfig config;
    private final AudioProperties audioProperties;
    private final AzureSttService sttService;

    public AzureAdapter(AzureConfig azureConfig,
                        AudioProperties audioProperties,
                        AzureSttService sttService) {
        this.config = azureConfig;
        this.audioProperties = audioProperties;
        this.sttService = sttService;
    }

    @Override
    public void startStreaming(String sessionId) {
        //TODO 따닥 문제 추후 고려
        if (sttService.isSessionActive(sessionId)) {
            log.warn("[{}] 이미 활성 세션이 존재합니다: {}", VENDOR_NAME, sessionId);
            return;
        }
        connect(sessionId);
    }

    private void connect(String sessionId) {
        try {
            SpeechConfig speechConfig = buildSpeechConfig();
            AudioStreamFormat format = AudioStreamFormat.getWaveFormatPCM(audioProperties.sampleRate(), (short) 16, (short) 1);
            PushAudioInputStream pushStream = AudioInputStream.createPushStream(format);
            AudioConfig audioConfig = AudioConfig.fromStreamInput(pushStream);
            ConversationTranscriber transcriber = new ConversationTranscriber(speechConfig, audioConfig);
            AzureSession session = new AzureSession(
                    sessionId,
                    transcriber,
                    pushStream,
                    audioConfig,
                    speechConfig
            );

            transcriber.transcribed.addEventListener((s, e) -> sttService.handleTranscribed(sessionId, e));
            transcriber.sessionStarted.addEventListener((s, e) -> sttService.handleSessionStarted(sessionId));
            transcriber.sessionStopped.addEventListener((s, e) -> sttService.handleSessionStopped(sessionId));
            transcriber.canceled.addEventListener((s, e) -> sttService.handleCanceled(sessionId, e));

            transcriber.startTranscribingAsync().get(3L, TimeUnit.SECONDS);
            sttService.handleStartStreaming(sessionId, session);

        } catch (Exception e) {
            log.error("[{}] 연결 실패: session={}, error={}", VENDOR_NAME, sessionId, e.getMessage(), e);
            throw new RuntimeException("Streaming Connection Failed"); //DebateTrackerException으로 변경 예정
        }
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

    @Override
    public void sendAudioChunk(String sessionId, byte[] pcmData) {
        sttService.handleSendAudioChunk(sessionId, pcmData);
    }

    @Override
    public void stopStreaming(String sessionId) {
        sttService.handleStopStreaming(sessionId);
    }

    @Override
    public boolean isConnected(String sessionId) {
        return sttService.isSessionActive(sessionId);
    }
}
