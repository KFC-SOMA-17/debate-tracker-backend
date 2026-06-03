package com.debatetracker.debate.service.debate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.service.BaseServiceTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DebateServiceTest extends BaseServiceTest {

    @Autowired
    private DebateService debateService;

    @Nested
    class Create {

        @Test
        void 토론을_생성하면_식별자가_부여된_토론을_반환한다() {
            Debate debate = new Debate(null, "인공지능은 인간의 일자리를 대체할 수 있는가");

            Debate actual = debateService.create(debate);

            assertAll(
                    () -> assertThat(actual.getId()).isNotNull(),
                    () -> assertThat(actual.getTopic()).isEqualTo("인공지능은 인간의 일자리를 대체할 수 있는가")
            );
        }
    }
}
