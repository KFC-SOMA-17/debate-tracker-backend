package com.debatetracker.debate.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DebateDomainRepositoryTest extends BaseDomainRepositoryTest {

    @Autowired
    private DebateDomainRepository debateDomainRepository;

    @Nested
    class Create {

        @Test
        void 토론을_저장하고_식별자가_부여된_도메인을_반환한다() {
            Debate debate = new Debate(null, "토론 주제");

            Debate created = debateDomainRepository.create(debate);

            assertAll(
                    () -> assertThat(created.getId()).isNotNull(),
                    () -> assertThat(created.getTopic()).isEqualTo("토론 주제")
            );
        }

        @Test
        void 저장한_토론이_실제로_영속화된다() {
            Debate debate = new Debate(null, "토론 주제");

            Debate created = debateDomainRepository.create(debate);

            DebateEntity persisted = debateJpaRepository.findById(created.getId()).orElseThrow();
            assertThat(persisted.getTopic()).isEqualTo("토론 주제");
        }
    }
}
