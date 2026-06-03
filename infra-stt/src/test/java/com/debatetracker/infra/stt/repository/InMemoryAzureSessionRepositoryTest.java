package com.debatetracker.infra.stt.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.debatetracker.infra.stt.session.AzureSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InMemoryAzureSessionRepositoryTest {

    private InMemoryAzureSessionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAzureSessionRepository();
    }

    @Nested
    class Save {

        @Test
        void 세션을_저장하면_조회할_수_있다() {
            AzureSession session = createMockSession("session-1");

            repository.save(session);

            assertThat(repository.findBySessionId("session-1")).isPresent();
        }

        @Test
        void 같은_sessionId로_저장하면_덮어쓴다() {
            AzureSession first = createMockSession("session-1");
            AzureSession second = createMockSession("session-1");

            repository.save(first);
            repository.save(second);

            assertThat(repository.findBySessionId("session-1").get()).isSameAs(second);
        }
    }

    @Nested
    class FindBySessionId {

        @Test
        void 존재하는_세션을_조회하면_Optional에_담겨_반환된다() {
            AzureSession session = createMockSession("session-1");
            repository.save(session);

            Optional<AzureSession> result = repository.findBySessionId("session-1");

            assertAll(
                    () -> assertThat(result).isPresent(),
                    () -> assertThat(result.get()).isSameAs(session)
            );
        }

        @Test
        void 존재하지_않는_세션을_조회하면_빈_Optional을_반환한다() {
            Optional<AzureSession> result = repository.findBySessionId("nonexistent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ExistsBySessionId {

        @Test
        void 존재하는_세션이면_true를_반환한다() {
            repository.save(createMockSession("session-1"));

            assertThat(repository.existsBySessionId("session-1")).isTrue();
        }

        @Test
        void 존재하지_않는_세션이면_false를_반환한다() {
            assertThat(repository.existsBySessionId("nonexistent")).isFalse();
        }

        @Test
        void 삭제된_세션이면_false를_반환한다() {
            repository.save(createMockSession("session-1"));
            repository.deleteBySessionId("session-1");

            assertThat(repository.existsBySessionId("session-1")).isFalse();
        }
    }

    @Nested
    class DeleteBySessionId {

        @Test
        void 세션을_삭제하면_삭제된_세션이_Optional에_담겨_반환된다() {
            AzureSession session = createMockSession("session-1");
            repository.save(session);

            Optional<AzureSession> deleted = repository.deleteBySessionId("session-1");

            assertAll(
                    () -> assertThat(deleted).isPresent(),
                    () -> assertThat(deleted.get()).isSameAs(session)
            );
        }

        @Test
        void 세션을_삭제하면_더_이상_조회되지_않는다() {
            AzureSession session = createMockSession("session-1");
            repository.save(session);

            repository.deleteBySessionId("session-1");

            assertThat(repository.findBySessionId("session-1")).isEmpty();
        }

        @Test
        void 존재하지_않는_세션을_삭제하면_빈_Optional을_반환한다() {
            Optional<AzureSession> deleted = repository.deleteBySessionId("nonexistent");

            assertThat(deleted).isEmpty();
        }
    }

    @Nested
    class MultiSession {

        @Test
        void 여러_세션이_독립적으로_관리된다() {
            AzureSession sessionA = createMockSession("session-a");
            AzureSession sessionB = createMockSession("session-b");
            repository.save(sessionA);
            repository.save(sessionB);

            repository.deleteBySessionId("session-a");

            assertAll(
                    () -> assertThat(repository.findBySessionId("session-a")).isEmpty(),
                    () -> assertThat(repository.findBySessionId("session-b")).isPresent()
            );
        }
    }

    private AzureSession createMockSession(String sessionId) {
        AzureSession session = mock(AzureSession.class);
        when(session.sessionId()).thenReturn(sessionId);
        return session;
    }
}
