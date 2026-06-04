package com.debatetracker.debate.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.infrastructure.persistence.DebateSessionDomainRepository;
import com.debatetracker.debate.infrastructure.persistence.inmemory.session.InMemoryDebateSessionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class DebateSessionDomainRepositoryTest {

    private InMemoryDebateSessionRepository sessionStore;
    private DebateSessionDomainRepository adapter;

    @BeforeEach
    void setUp() {
        sessionStore = mock(InMemoryDebateSessionRepository.class);
        adapter = new DebateSessionDomainRepository(sessionStore);
    }

    @Nested
    class Save {

        @Test
        void save는_저장소에_위임한다() {
            String debateId = "1";
            DebateSession session = new DebateSession(debateId, mock(WebSocketSession.class));

            adapter.save(session);

            verify(sessionStore).save(session);
        }
    }

    @Nested
    class FindByDebateId {

        @Test
        void findByDebateId는_저장소_결과를_그대로_반환한다() {
            String debateId = "1";
            DebateSession session = new DebateSession(debateId, mock(WebSocketSession.class));
            when(sessionStore.findByDebateId(debateId)).thenReturn(Optional.of(session));

            assertThat(adapter.findByDebateId(debateId)).get().isSameAs(session);
        }
    }

    @Nested
    class ExistsByDebateId {

        @Test
        void existsByDebateId는_저장소_결과를_그대로_반환한다() {
            String debateId = "1";
            when(sessionStore.existsByDebateId(debateId)).thenReturn(true);

            assertThat(adapter.existsByDebateId(debateId)).isTrue();
        }
    }

    @Nested
    class DeleteByDebateId {

        @Test
        void deleteByDebateId는_저장소_결과를_그대로_반환한다() {
            String debateId = "1";
            DebateSession session = new DebateSession(debateId, mock(WebSocketSession.class));
            when(sessionStore.deleteByDebateId(debateId)).thenReturn(Optional.of(session));

            assertThat(adapter.deleteByDebateId(debateId)).get().isSameAs(session);
        }
    }
}
