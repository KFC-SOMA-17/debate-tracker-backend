package com.debatetracker.debate.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.domain.transcript.SpeechBox;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class SpeechBoxDomainRepositoryTest extends BaseDomainRepositoryTest {

    private static final long DEBATE_ID = 1L;

    @Autowired
    private SpeechBoxDomainRepository speechBoxDomainRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Nested
    class SaveAll {

        @Test
        void 신규_박스들을_한_번에_저장한다() {
            List<SpeechBox> boxes = List.of(
                    box("S1", "첫 발화", "0.0", "2.0"),
                    box("S2", "두번째 발화", "2.0", "4.0"),
                    box("S3", "세번째 발화", "4.0", "6.0")
            );

            speechBoxDomainRepository.saveAll(boxes);

            assertThat(countByDebateId(DEBATE_ID)).isEqualTo(3L);
        }
    }

    @Nested
    class FindLastByDebateId {

        @Test
        void 저장된_박스가_없으면_빈값을_반환한다() {
            assertThat(speechBoxDomainRepository.findLastByDebateId(DEBATE_ID)).isEmpty();
        }

        @Test
        void 가장_나중에_저장된_박스를_반환한다() {
            speechBoxDomainRepository.saveAll(List.of(
                    box("S1", "먼저", "0.0", "2.0"),
                    box("S2", "나중", "2.0", "4.0")
            ));

            SpeechBox last = speechBoxDomainRepository.findLastByDebateId(DEBATE_ID).orElseThrow();

            assertAll(
                    () -> assertThat(last.getSpeaker()).isEqualTo("S2"),
                    () -> assertThat(last.getContent()).isEqualTo("나중"),
                    () -> assertThat(last.getStartAt()).isEqualByComparingTo(new BigDecimal("2.0")),
                    () -> assertThat(last.getEndAt()).isEqualByComparingTo(new BigDecimal("4.0"))
            );
        }
    }

    @Nested
    class Update {

        @Test
        void 기존_박스의_content와_endAt을_갱신한다() {
            speechBoxDomainRepository.saveAll(List.of(box("S1", "원래 내용", "0.0", "2.0")));
            SpeechBox persisted = speechBoxDomainRepository.findLastByDebateId(DEBATE_ID).orElseThrow();
            SpeechBox merged = new SpeechBox(
                    persisted.getId(), DEBATE_ID, "S1", "원래 내용 이어붙인 내용",
                    persisted.getStartAt(), new BigDecimal("5.0"));

            speechBoxDomainRepository.update(merged);

            SpeechBox updated = speechBoxDomainRepository.findLastByDebateId(DEBATE_ID).orElseThrow();
            assertAll(
                    () -> assertThat(countByDebateId(DEBATE_ID)).isEqualTo(1L),
                    () -> assertThat(updated.getId()).isEqualTo(persisted.getId()),
                    () -> assertThat(updated.getContent()).isEqualTo("원래 내용 이어붙인 내용"),
                    () -> assertThat(updated.getEndAt()).isEqualByComparingTo(new BigDecimal("5.0"))
            );
        }
    }

    private long countByDebateId(long debateId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM speech_box WHERE debate_id = :debateId",
                new MapSqlParameterSource("debateId", debateId),
                Long.class);
        return count == null ? 0L : count;
    }

    private SpeechBox box(String speaker, String content, String startAt, String endAt) {
        return new SpeechBox(null, DEBATE_ID, speaker, content, new BigDecimal(startAt), new BigDecimal(endAt));
    }
}
