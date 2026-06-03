package com.debatetracker.debate.ws.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.domain.transcript.SpeechSegment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WebSocketMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void debateStart_팩토리는_data가_null이다() {
        WebSocketMessage message = new DebateStartMessage(1L);

        assertAll(
                () -> assertThat(message.type()).isEqualTo(MessageType.DEBATE_START),
                () -> assertThat(message.debateId()).isEqualTo(1L),
                () -> assertThat(message.data()).isNull()
        );
    }

    @Test
    void transcription_팩토리는_세그먼트를_data로_감싼다() {
        SpeechSegment segment = new SpeechSegment(
                "100", "안녕하세요", "Guest_1", new BigDecimal("1.200"), new BigDecimal("4.800"));

        WebSocketMessage message = new TranscriptionMessage(1L, segment);

        assertAll(
                () -> assertThat(message.type()).isEqualTo(MessageType.TRANSCRIPTION),
                () -> assertThat(message.data()).isSameAs(segment)
        );
    }

    @Test
    void TRANSCRIPTION_메시지는_camelCase_필드와_문자열_id로_직렬화된다() throws Exception {
        SpeechSegment segment = new SpeechSegment(
                "7202948293847291904", "안녕하세요", "Guest_1",
                new BigDecimal("1.200"), new BigDecimal("4.800"));

        String json = objectMapper.writeValueAsString(new TranscriptionMessage(1L, segment));

        assertAll(
                () -> assertThat(json).contains("\"debateId\":1"),
                () -> assertThat(json).contains("\"type\":\"TRANSCRIPTION\""),
                () -> assertThat(json).contains("\"id\":\"7202948293847291904\""),
                () -> assertThat(json).contains("\"startAt\":1.200"),
                () -> assertThat(json).contains("\"endAt\":4.800")
        );
    }
}
