package com.debatetracker.infra.llm.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RefineRequestTest {

    @Nested
    class FindTargetSegment {

        @Test
        void 대상_세그먼트를_찾으면_반환한다() {
            RefineRequest request = request(List.of(segment("1"), segment("2")));

            Optional<TranscriptSegment> found = request.findTargetSegment("2");

            assertAll(
                    () -> assertThat(found).isPresent(),
                    () -> assertThat(found.get().id()).isEqualTo("2")
            );

        }

        @Test
        void 대상_세그먼트가_없으면_빈_값을_반환한다() {
            RefineRequest request = request(List.of(segment("1")));

            assertThat(request.findTargetSegment("99")).isEmpty();
        }
    }

    @Nested
    class Targets {

        @Test
        void 대상_목록은_방어적으로_복사된다() {
            List<TranscriptSegment> source = new ArrayList<>(List.of(segment("1")));
            RefineRequest request = request(source);

            source.add(segment("2"));

            assertThat(request.targets()).hasSize(1);
        }

        @Test
        void 대상_목록은_수정할_수_없다() {
            RefineRequest request = request(List.of(segment("1")));

            assertThatThrownBy(() -> request.targets().add(segment("2")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    private static RefineRequest request(List<TranscriptSegment> targets) {
        return new RefineRequest("session-1", "주제", List.of(), targets);
    }

    private static TranscriptSegment segment(String id) {
        return new TranscriptSegment(id, "A", new BigDecimal("0.0"), new BigDecimal("1.0"), "발화");
    }
}
