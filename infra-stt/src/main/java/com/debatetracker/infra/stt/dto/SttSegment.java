package com.debatetracker.infra.stt.dto;

import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriptionResult;
import java.math.BigDecimal;

public record SttSegment(
        BigDecimal start,
        BigDecimal end,
        String speaker,
        String content
) {

    public static SttSegment fromAzureResult(ConversationTranscriptionResult result) {
        long offsetTicks = result.getOffset().longValue();
        long durationTicks = result.getDuration().longValue();
        return new SttSegment(
                BigDecimal.valueOf(offsetTicks / 10_000_000.0),
                BigDecimal.valueOf(((offsetTicks + durationTicks) / 10_000_000.0)),
                result.getSpeakerId(),
                result.getText()
        );
    }
}
