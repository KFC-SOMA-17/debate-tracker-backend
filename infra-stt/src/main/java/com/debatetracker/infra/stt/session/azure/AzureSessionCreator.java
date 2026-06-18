package com.debatetracker.infra.stt.session.azure;

import com.debatetracker.infra.stt.router.SttSession;
import com.debatetracker.infra.stt.router.SttSessionCreator;
import com.debatetracker.infra.stt.config.AudioProperties;
import com.debatetracker.infra.stt.config.AzureConfig;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@RequiredArgsConstructor
public class AzureSessionCreator implements SttSessionCreator {

    private final AzureConfig azureConfig;
    private final AudioProperties audioProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public SttSession create(String sessionId) {
        SpeechConfig speechConfig = null;
        PushAudioInputStream pushStream = null;
        AudioConfig audioConfig = null;
        ConversationTranscriber transcriber = null;

        try {
            speechConfig = buildSpeechConfig();
            AudioStreamFormat format = AudioStreamFormat.getWaveFormatPCM(
                    audioProperties.sampleRate(), (short) 16, (short) 1);
            pushStream = AudioInputStream.createPushStream(format);
            audioConfig = AudioConfig.fromStreamInput(pushStream);
            transcriber = new ConversationTranscriber(speechConfig, audioConfig);

            AzureSttSession session = new AzureSttSession(
                    sessionId, transcriber, pushStream, audioConfig, speechConfig, eventPublisher);

            transcriber.startTranscribingAsync().get(3L, TimeUnit.SECONDS);
            log.info("[azure] 전사 시작 성공, session={}", sessionId);
            return session;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[azure] 연결 실패: session={}, error={}", sessionId, e.getMessage(), e);
            AzureUtils.close(pushStream);
            AzureUtils.close(transcriber);
            AzureUtils.close(audioConfig);
            AzureUtils.close(speechConfig);
            throw new RuntimeException("Streaming Connection Failed", e);
        } catch (Exception e) {
            log.error("[azure] 연결 실패: session={}, error={}", sessionId, e.getMessage(), e);
            AzureUtils.close(pushStream);
            AzureUtils.close(transcriber);
            AzureUtils.close(audioConfig);
            AzureUtils.close(speechConfig);
            throw new RuntimeException("Streaming Connection Failed", e);
        }
    }

    private SpeechConfig buildSpeechConfig() {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(
                azureConfig.subscriptionKey(), azureConfig.region());
        speechConfig.setSpeechRecognitionLanguage(azureConfig.language());
        speechConfig.setProfanity(ProfanityOption.Raw);
        speechConfig.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs,
                String.valueOf(azureConfig.silenceTimeoutMs()));
        speechConfig.enableDictation();
        speechConfig.setOutputFormat(OutputFormat.Detailed);
        return speechConfig;
    }
}
