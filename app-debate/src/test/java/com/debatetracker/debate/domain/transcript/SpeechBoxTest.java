package com.debatetracker.debate.domain.transcript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SpeechBoxTest {

    private static final long DEBATE_ID = 1L;

    @Nested
    class Append {

        @Test
        void 식별자와_시작시각은_유지하고_content는_이어붙이며_끝시각은_갱신한다() {
            SpeechBox lastBox = new SpeechBox(
                    7L, DEBATE_ID, "S1", "안녕하세요 저는 김건우입니다", time("0.0"), time("3.0"));
            SpeechBox next = new SpeechBox(
                    null, DEBATE_ID, "S1", "오늘 이야기해볼 주제는 동물실험입니다", time("3.0"), time("8.0"));

            SpeechBox merged = lastBox.append(next);

            assertAll(
                    () -> assertThat(merged.getId()).isEqualTo(7L),
                    () -> assertThat(merged.getSpeaker()).isEqualTo("S1"),
                    () -> assertThat(merged.getContent())
                            .isEqualTo("안녕하세요 저는 김건우입니다 오늘 이야기해볼 주제는 동물실험입니다"),
                    () -> assertThat(merged.getStartAt()).isEqualByComparingTo(time("0.0")),
                    () -> assertThat(merged.getEndAt()).isEqualByComparingTo(time("8.0"))
            );
        }
    }

    private BigDecimal time(String value) {
        return new BigDecimal(value);
    }
}
