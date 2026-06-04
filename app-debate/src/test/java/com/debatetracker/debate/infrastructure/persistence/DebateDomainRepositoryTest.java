package com.debatetracker.debate.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.fixture.DebateGenerator;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateEntity;
import com.debatetracker.exception.DebateTrackerException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DebateDomainRepositoryTest extends BaseDomainRepositoryTest {

    @Autowired
    private DebateDomainRepository debateDomainRepository;

    @Autowired
    private DebateGenerator debateGenerator;

    @Nested
    class Create {

        @Test
        void 토론을_저장하고_식별자가_부여된_도메인을_반환한다() {
            String topic = "토론 주제";

            Debate created = debateDomainRepository.create(new Debate(null, topic));

            assertAll(
                    () -> assertThat(created.getId()).isNotNull(),
                    () -> assertThat(created.getTopic()).isEqualTo(topic)
            );
        }

        @Test
        void 저장한_토론이_실제로_영속화된다() {
            String topic = "토론 주제";

            Debate created = debateDomainRepository.create(new Debate(null, topic));

            DebateEntity persisted = debateJpaRepository.findById(created.getId()).orElseThrow();
            assertThat(persisted.getTopic()).isEqualTo(topic);
        }
    }

    @Nested
    class FindById {

        @Test
        void DB에서_토론을_조회해_반환한다() {
            String topic = "토론 주제";

            Debate created = debateGenerator.generate(topic);

            Debate found = debateDomainRepository.findById(created.getId());

            assertAll(
                    () -> assertThat(found.getId()).isEqualTo(created.getId()),
                    () -> assertThat(found.getTopic()).isEqualTo(topic)
            );
        }

        @Test
        void 존재하지_않는_토론은_예외를_던진다() {
            long missingDebateId = 999_999_999L;

            assertThatThrownBy(() -> debateDomainRepository.findById(missingDebateId))
                    .isInstanceOf(DebateTrackerException.class);
        }
    }
}
