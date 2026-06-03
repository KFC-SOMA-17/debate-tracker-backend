package com.debatetracker.debate.ws.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class InMemoryDebateSessionRepositoryTest {

    private InMemoryDebateSessionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDebateSessionRepository();
    }

    @Nested
    class Save {

        @Test
        void 세션을_저장하면_조회할_수_있다() {
            repository.save(createSession("1"));

            assertThat(repository.findByDebateId("1")).isPresent();
        }

        @Test
        void 같은_debateId로_저장하면_덮어쓴다() {
            DebateSession first = createSession("1");
            DebateSession second = createSession("1");
            repository.save(first);
            repository.save(second);

            assertThat(repository.findByDebateId("1")).get().isSameAs(second);
        }
    }

    @Nested
    class FindByDebateId {

        @Test
        void 존재하는_세션을_조회하면_Optional에_담겨_반환된다() {
            DebateSession session = createSession("1");
            repository.save(session);

            Optional<DebateSession> result = repository.findByDebateId("1");

            assertAll(
                    () -> assertThat(result).isPresent(),
                    () -> assertThat(result.get()).isSameAs(session)
            );
        }

        @Test
        void 존재하지_않는_세션을_조회하면_빈_Optional을_반환한다() {
            assertThat(repository.findByDebateId("none")).isEmpty();
        }
    }

    @Nested
    class ExistsByDebateId {

        @Test
        void 존재하는_세션이면_true를_반환한다() {
            repository.save(createSession("1"));

            assertThat(repository.existsByDebateId("1")).isTrue();
        }

        @Test
        void 존재하지_않는_세션이면_false를_반환한다() {
            assertThat(repository.existsByDebateId("none")).isFalse();
        }

        @Test
        void 삭제된_세션이면_false를_반환한다() {
            repository.save(createSession("1"));
            repository.deleteByDebateId("1");

            assertThat(repository.existsByDebateId("1")).isFalse();
        }
    }

    @Nested
    class DeleteByDebateId {

        @Test
        void 세션을_삭제하면_삭제된_세션이_Optional에_담겨_반환된다() {
            DebateSession session = createSession("1");
            repository.save(session);

            Optional<DebateSession> deleted = repository.deleteByDebateId("1");

            assertAll(
                    () -> assertThat(deleted).isPresent(),
                    () -> assertThat(deleted.get()).isSameAs(session)
            );
        }

        @Test
        void 세션을_삭제하면_더_이상_조회되지_않는다() {
            repository.save(createSession("1"));
            repository.deleteByDebateId("1");

            assertThat(repository.findByDebateId("1")).isEmpty();
        }

        @Test
        void 존재하지_않는_세션을_삭제하면_빈_Optional을_반환한다() {
            assertThat(repository.deleteByDebateId("none")).isEmpty();
        }
    }

    @Nested
    class MultiSession {

        @Test
        void 여러_세션이_독립적으로_관리된다() {
            repository.save(createSession("a"));
            repository.save(createSession("b"));

            repository.deleteByDebateId("a");

            assertAll(
                    () -> assertThat(repository.findByDebateId("a")).isEmpty(),
                    () -> assertThat(repository.findByDebateId("b")).isPresent()
            );
        }
    }

    private DebateSession createSession(String debateId) {
        return new DebateSession(debateId, mock(WebSocketSession.class));
    }
}
