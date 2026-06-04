package com.debatetracker.debate.infrastructure.persistence.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.config.TestcontainersConfiguration;
import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TranscriptBufferRedisRepositoryTest {

    private static final String DEBATE_ID = "990001";

    @Autowired
    private TranscriptBufferRedisRepository repository;

    @BeforeEach
    @AfterEach
    void clearBuffer() {
        repository.clear(DEBATE_ID);
    }

    @Nested
    class AppendRaw {

        @Test
        void raw_세그먼트를_적재하고_골격을_보존한_채_조회한다() {
            repository.appendRaw(DEBATE_ID, speech("a", "안녕하세요"));
            repository.appendRaw(DEBATE_ID, speech("b", "반갑습니다"));

            List<SpeechSegment> peeked = repository.peekRaw(DEBATE_ID, 2);

            assertAll(
                    () -> assertThat(repository.countRawSegments(DEBATE_ID)).isEqualTo(2L),
                    () -> assertThat(peeked).extracting(SpeechSegment::getId).containsExactly("a", "b"),
                    () -> assertThat(peeked.get(0).getContent()).isEqualTo("안녕하세요"),
                    () -> assertThat(peeked.get(0).getSpeaker()).isEqualTo("Guest_0"),
                    () -> assertThat(peeked.get(0).getStartAt()).isEqualByComparingTo("1.0"),
                    () -> assertThat(peeked.get(0).getEndAt()).isEqualByComparingTo("2.0")
            );
        }
    }

    @Nested
    class TrimRaw {

        @Test
        void trimRaw는_앞쪽_count개만_제거한다() {
            repository.appendRaw(DEBATE_ID, speech("a", "하나"));
            repository.appendRaw(DEBATE_ID, speech("b", "둘"));
            repository.appendRaw(DEBATE_ID, speech("c", "셋"));

            repository.trimRaw(DEBATE_ID, 2);

            List<SpeechSegment> remaining = repository.peekRaw(DEBATE_ID, 10);
            assertAll(
                    () -> assertThat(repository.countRawSegments(DEBATE_ID)).isEqualTo(1L),
                    () -> assertThat(remaining).extracting(SpeechSegment::getId).containsExactly("c")
            );
        }
    }

    @Nested
    class AppendRefined {

        @Test
        void refined를_뒤에_축적하고_최근_n개만_조회한다() {
            repository.appendRefined(DEBATE_ID, List.of(refined("a"), refined("b"), refined("c")));
            repository.appendRefined(DEBATE_ID, List.of(refined("d")));

            List<RefinedSpeechSegment> recent = repository.recentRefined(DEBATE_ID, 2);

            assertThat(recent).extracting(RefinedSpeechSegment::getId).containsExactly("c", "d");
        }
    }

    @Nested
    class EmptyBuffer {

        @Test
        void 빈_버퍼_조회는_빈_리스트를_반환한다() {
            assertAll(
                    () -> assertThat(repository.countRawSegments(DEBATE_ID)).isZero(),
                    () -> assertThat(repository.peekRaw(DEBATE_ID, 5)).isEmpty(),
                    () -> assertThat(repository.recentRefined(DEBATE_ID, 5)).isEmpty()
            );
        }
    }

    private SpeechSegment speech(String id, String content) {
        return new SpeechSegment(id, content, "Guest_0", new BigDecimal("1.0"), new BigDecimal("2.0"));
    }

    private RefinedSpeechSegment refined(String id) {
        return new RefinedSpeechSegment(id, "교정 " + id, "Guest_0", new BigDecimal("1.0"), new BigDecimal("2.0"));
    }
}
