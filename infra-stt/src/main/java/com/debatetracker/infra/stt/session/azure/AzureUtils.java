package com.debatetracker.infra.stt.session.azure;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriber;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AzureUtils {

    private static final long STOP_TIMEOUT_SECONDS = 3L;

    public static void close(PushAudioInputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (Exception e) {
            log.warn("[azure-utils] PushAudioInputStream 닫기 실패: {}", e.getMessage());
        }
    }

    public static void close(ConversationTranscriber transcriber) {
        if (transcriber == null) {
            return;
        }
        try {
            transcriber.stopTranscribingAsync().get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[azure-utils] ConversationTranscriber 중지 실패: {}", e.getMessage());
        }
        try {
            transcriber.close();
        } catch (Exception e) {
            log.warn("[azure-utils] ConversationTranscriber 닫기 실패: {}", e.getMessage());
        }
    }

    public static void close(AudioConfig config) {
        if (config == null) {
            return;
        }
        try {
            config.close();
        } catch (Exception e) {
            log.warn("[azure-utils] AudioConfig 닫기 실패: {}", e.getMessage());
        }
    }

    public static void close(SpeechConfig config) {
        if (config == null) {
            return;
        }
        try {
            config.close();
        } catch (Exception e) {
            log.warn("[azure-utils] SpeechConfig 닫기 실패: {}", e.getMessage());
        }
    }
}
