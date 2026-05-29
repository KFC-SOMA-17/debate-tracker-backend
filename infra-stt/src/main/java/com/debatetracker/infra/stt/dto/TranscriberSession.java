package com.debatetracker.infra.stt.dto;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriber;
import java.util.function.Consumer;

public record TranscriberSession(
        String sessionId,
        ConversationTranscriber transcriber,
        PushAudioInputStream pushStream,
        AudioConfig audioConfig,
        SpeechConfig speechConfig,
        Consumer<SttSegment> callback
) {

    public void close() {
        try { pushStream.close(); } catch (Exception e) { /* ignore */ }
        try { transcriber.stopTranscribingAsync().get(); } catch (Exception e) { /* ignore */ }
        transcriber.close();
        audioConfig.close();
        speechConfig.close();
    }
}
