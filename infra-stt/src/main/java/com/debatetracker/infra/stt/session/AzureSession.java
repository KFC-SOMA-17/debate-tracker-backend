package com.debatetracker.infra.stt.session;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriber;
import java.util.concurrent.TimeUnit;

public record AzureSession(
        String sessionId,
        ConversationTranscriber transcriber,
        PushAudioInputStream pushStream,
        AudioConfig audioConfig,
        SpeechConfig speechConfig
) {

    //TODO close 장기 지연 문제 추후 해결
    public void close() {
        try {
            pushStream.close();
            transcriber.stopTranscribingAsync().get(3L, TimeUnit.SECONDS);
        } catch (Exception e) { /* ignore */ }
        transcriber.close();
        audioConfig.close();
        speechConfig.close();
    }
}
