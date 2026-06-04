package com.debatetracker.debate.service.transcript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechBox;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SpeechBoxResolverTest {

    private static final long DEBATE_ID = 1L;

    private final SpeechBoxResolver resolver = new SpeechBoxResolver();

    @Nested
    class Resolve {

        @Test
        void 연속된_같은_화자_세그먼트를_하나의_박스로_묶고_화자가_바뀌면_새_박스로_나눈다() {
            List<RefinedSpeechSegment> segments = List.of(
                    refined("a", "S1", "오늘 이야기해볼 주제는", "3.0", "5.0"),
                    refined("b", "S1", "동물실험입니다. 찬성측부터 시작하시죠", "5.0", "8.0"),
                    refined("c", "S2", "네 찬성측 입론 시작하겠습니다.", "8.0", "11.0"),
                    refined("d", "S2", "찬성측 입론 마치겠습니다", "11.0", "13.0"),
                    refined("e", "S3", "이번엔 반대측 입론이군요", "13.0", "15.0")
            );

            List<SpeechBox> boxes = resolver.resolve(DEBATE_ID, segments);

            SpeechBox first = boxes.get(0);
            SpeechBox second = boxes.get(1);
            SpeechBox third = boxes.get(2);
            assertAll(
                    () -> assertThat(boxes).hasSize(3),
                    () -> assertThat(boxes).allSatisfy(box -> {
                        assertThat(box.getId()).isNull();
                        assertThat(box.getDebateId()).isEqualTo(DEBATE_ID);
                    }),
                    () -> assertThat(first.getSpeaker()).isEqualTo("S1"),
                    () -> assertThat(first.getContent()).isEqualTo("오늘 이야기해볼 주제는 동물실험입니다. 찬성측부터 시작하시죠"),
                    () -> assertThat(first.getStartAt()).isEqualByComparingTo(time("3.0")),
                    () -> assertThat(first.getEndAt()).isEqualByComparingTo(time("8.0")),
                    () -> assertThat(second.getSpeaker()).isEqualTo("S2"),
                    () -> assertThat(second.getContent()).isEqualTo("네 찬성측 입론 시작하겠습니다. 찬성측 입론 마치겠습니다"),
                    () -> assertThat(second.getStartAt()).isEqualByComparingTo(time("8.0")),
                    () -> assertThat(second.getEndAt()).isEqualByComparingTo(time("13.0")),
                    () -> assertThat(third.getSpeaker()).isEqualTo("S3"),
                    () -> assertThat(third.getContent()).isEqualTo("이번엔 반대측 입론이군요")
            );
        }

        @Test
        void 모든_세그먼트가_같은_화자면_하나의_박스로_묶는다() {
            List<RefinedSpeechSegment> segments = List.of(
                    refined("a", "S1", "첫 발화", "0.0", "2.0"),
                    refined("b", "S1", "이어지는 발화", "2.0", "4.0")
            );

            List<SpeechBox> boxes = resolver.resolve(DEBATE_ID, segments);

            assertAll(
                    () -> assertThat(boxes).hasSize(1),
                    () -> assertThat(boxes.get(0).getContent()).isEqualTo("첫 발화 이어지는 발화"),
                    () -> assertThat(boxes.get(0).getStartAt()).isEqualByComparingTo(time("0.0")),
                    () -> assertThat(boxes.get(0).getEndAt()).isEqualByComparingTo(time("4.0"))
            );
        }

        @Test
        void 빈_세그먼트면_빈_리스트를_반환한다() {
            assertThat(resolver.resolve(DEBATE_ID, List.of())).isEmpty();
        }
    }

    private RefinedSpeechSegment refined(String id, String speaker, String content, String startAt, String endAt) {
        return new RefinedSpeechSegment(id, content, speaker, time(startAt), time(endAt));
    }

    private BigDecimal time(String value) {
        return new BigDecimal(value);
    }
}
