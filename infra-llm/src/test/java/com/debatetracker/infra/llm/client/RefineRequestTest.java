package com.debatetracker.infra.llm.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RefineRequestTest {

    @Test
    void findTargetSegment_present() {
        RefineRequest request = request(List.of(segment("1"), segment("2")));

        Optional<TranscriptSegment> found = request.findTargetSegment("2");

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo("2");
    }

    @Test
    void findTargetSegment_absent() {
        RefineRequest request = request(List.of(segment("1")));

        assertThat(request.findTargetSegment("99")).isEmpty();
    }

    @Test
    void targets_defensivelyCopied() {
        List<TranscriptSegment> source = new ArrayList<>(List.of(segment("1")));
        RefineRequest request = request(source);

        source.add(segment("2"));

        assertThat(request.targets()).hasSize(1);
    }

    @Test
    void targets_immutable() {
        RefineRequest request = request(List.of(segment("1")));

        assertThatThrownBy(() -> request.targets().add(segment("2")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static RefineRequest request(List<TranscriptSegment> targets) {
        return new RefineRequest("session-1", "주제", List.of(), targets);
    }

    private static TranscriptSegment segment(String id) {
        return new TranscriptSegment(id, "A", new BigDecimal("0.0"), new BigDecimal("1.0"), "발화");
    }
}
