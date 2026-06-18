package com.debatetracker.infra.stt.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

import com.debatetracker.infra.stt.router.SttSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InMemorySttSessionRepositoryTest {

    private InMemorySttSessionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemorySttSessionRepository();
    }

    @Nested
    class Save {

        @Test
        void 세션을_저장하면_조회할_수_있다() {
            SttSession session = mock(SttSession.class);

            repository.save("session-1", session);

            assertThat(repository.findBySessionId("session-1")).isPresent();
        }

        @Test
        void 같은_sessionId로_저장하면_덮어쓴다() {
            SttSession first = mock(SttSession.class);
            SttSession second = mock(SttSession.class);

            repository.save("session-1", first);
            repository.save("session-1", second);

            assertThat(repository.findBySessionId("session-1").get()).isSameAs(second);
        }
    }

    @Nested
    class FindBySessionId {

        @Test
        void 존재하는_세션을_조회하면_Optional에_담겨_반환된다() {
            SttSession session = mock(SttSession.class);
            repository.save("session-1", session);

            Optional<SttSession> result = repository.findBySessionId("session-1");

            assertAll(
                    () -> assertThat(result).isPresent(),
                    () -> assertThat(result.get()).isSameAs(session)
            );
        }

        @Test
        void 존재하지_않는_세션을_조회하면_빈_Optional을_반환한다() {
            Optional<SttSession> result = repository.findBySessionId("nonexistent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ExistsBySessionId {

        @Test
        void 존재하는_세션이면_true를_반환한다() {
            repository.save("session-1", mock(SttSession.class));

            assertThat(repository.existsBySessionId("session-1")).isTrue();
        }

        @Test
        void 존재하지_않는_세션이면_false를_반환한다() {
            assertThat(repository.existsBySessionId("nonexistent")).isFalse();
        }

        @Test
        void 삭제된_세션이면_false를_반환한다() {
            repository.save("session-1", mock(SttSession.class));
            repository.deleteBySessionId("session-1");

            assertThat(repository.existsBySessionId("session-1")).isFalse();
        }
    }

    @Nested
    class DeleteBySessionId {

        @Test
        void 세션을_삭제하면_삭제된_세션이_Optional에_담겨_반환된다() {
            SttSession session = mock(SttSession.class);
            repository.save("session-1", session);

            Optional<SttSession> deleted = repository.deleteBySessionId("session-1");

            assertAll(
                    () -> assertThat(deleted).isPresent(),
                    () -> assertThat(deleted.get()).isSameAs(session)
            );
        }

        @Test
        void 세션을_삭제하면_더_이상_조회되지_않는다() {
            repository.save("session-1", mock(SttSession.class));

            repository.deleteBySessionId("session-1");

            assertThat(repository.findBySessionId("session-1")).isEmpty();
        }

        @Test
        void 존재하지_않는_세션을_삭제하면_빈_Optional을_반환한다() {
            Optional<SttSession> deleted = repository.deleteBySessionId("nonexistent");

            assertThat(deleted).isEmpty();
        }
    }

    @Nested
    class MultiSession {

        @Test
        void 여러_세션이_독립적으로_관리된다() {
            SttSession sessionA = mock(SttSession.class);
            SttSession sessionB = mock(SttSession.class);
            repository.save("session-a", sessionA);
            repository.save("session-b", sessionB);

            repository.deleteBySessionId("session-a");

            assertAll(
                    () -> assertThat(repository.findBySessionId("session-a")).isEmpty(),
                    () -> assertThat(repository.findBySessionId("session-b")).isPresent()
            );
        }
    }
}
