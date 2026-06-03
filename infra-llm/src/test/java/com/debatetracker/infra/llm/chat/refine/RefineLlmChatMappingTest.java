package com.debatetracker.infra.llm.chat.refine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.infra.llm.client.TranscriptSegment;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RefineLlmChatMappingTest {

    @Test
    void from_keepsTextFields_dropsTimestamps() {
        TranscriptSegment segment = new TranscriptSegment(
                "1", "A", new BigDecimal("1.5"), new BigDecimal("2.5"), "발화");

        RefineLlmChatRequest request = RefineLlmChatRequest.from(List.of(segment));

        assertAll(
                () -> assertThat(request.segments()).hasSize(1),
                () -> assertThat(request.segments().getFirst().id()).isEqualTo("1"),
                () -> assertThat(request.segments().getFirst().speaker()).isEqualTo("A"),
                () -> assertThat(request.segments().getFirst().text()).isEqualTo("발화")
        );

    }

    @Test
    void constructor_copiesTextFields() {
        TranscriptSegment segment = new TranscriptSegment(
                "1", "A", new BigDecimal("1.5"), new BigDecimal("2.5"), "발화");

        RefineLlmChatSegment mapped = new RefineLlmChatSegment(segment);

        assertAll(
                () -> assertThat(mapped.id()).isEqualTo("1"),
                () -> assertThat(mapped.speaker()).isEqualTo("A"),
                () -> assertThat(mapped.text()).isEqualTo("발화")
        );

    }

    @Test
    void toTranscriptSegment_restoresWithGivenTimestamps() {
        RefineLlmChatSegment segment = new RefineLlmChatSegment("1", "A", "정제됨");

        TranscriptSegment restored = segment.toTranscriptSegment(new BigDecimal("4.0"), new BigDecimal("5.0"));

        assertAll(
                () -> assertThat(restored.id()).isEqualTo("1"),
                () -> assertThat(restored.speaker()).isEqualTo("A"),
                () -> assertThat(restored.text()).isEqualTo("정제됨"),
                () -> assertThat(restored.start()).isEqualByComparingTo(new BigDecimal("4.0")),
                () -> assertThat(restored.end()).isEqualByComparingTo(new BigDecimal("5.0"))
        );
    }
}
