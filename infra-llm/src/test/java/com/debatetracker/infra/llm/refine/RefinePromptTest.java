package com.debatetracker.infra.llm.refine;

import static org.assertj.core.api.Assertions.assertThat;

import com.debatetracker.infra.llm.client.TranscriptSegment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefinePromptTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("유저 프롬프트에 모든 세그먼트의 id 와 text 가 포함된다")
    void buildUserPrompt_containsAllSegments() {
        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment("1", "Spk1", new BigDecimal("1.0"), new BigDecimal("2.0"), "안녕"),
                new TranscriptSegment("2", "Spk2", new BigDecimal("2.0"), new BigDecimal("3.0"), "반가워"));

        String prompt = RefinePrompt.buildUserPrompt(objectMapper, segments);

        assertThat(prompt).contains("\"1\"").contains("안녕");
        assertThat(prompt).contains("\"2\"").contains("반가워");
    }

    @Test
    @DisplayName("유저 프롬프트에는 start/end 타임스탬프를 넣지 않는다")
    void buildUserPrompt_excludesTimestamps() {
        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment("1", "Spk1", new BigDecimal("1.680"), new BigDecimal("3.540"), "안녕"));

        String prompt = RefinePrompt.buildUserPrompt(objectMapper, segments);

        assertThat(prompt).doesNotContain("1.680").doesNotContain("3.540");
    }

    @Test
    @DisplayName("시스템 프롬프트는 JSON-only 지시와 id 불변식을 포함한다")
    void systemPrompt_hasJsonOnlyAndInvariants() {
        assertThat(RefinePrompt.SYSTEM)
                .contains("JSON")
                .contains("id");
    }
}
