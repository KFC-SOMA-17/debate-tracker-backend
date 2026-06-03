package com.debatetracker.debate.ws.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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

    @Test
    void save는_저장소에_위임한다() {
        DebateSession session = new DebateSession("1", mock(WebSocketSession.class));

        adapter.save(session);

        verify(sessionStore).save(session);
    }

    @Test
    void findByDebateId는_저장소_결과를_그대로_반환한다() {
        DebateSession session = new DebateSession("1", mock(WebSocketSession.class));
        when(sessionStore.findByDebateId("1")).thenReturn(Optional.of(session));

        assertThat(adapter.findByDebateId("1")).get().isSameAs(session);
    }

    @Test
    void existsByDebateId는_저장소_결과를_그대로_반환한다() {
        when(sessionStore.existsByDebateId("1")).thenReturn(true);

        assertThat(adapter.existsByDebateId("1")).isTrue();
    }

    @Test
    void deleteByDebateId는_저장소_결과를_그대로_반환한다() {
        DebateSession session = new DebateSession("1", mock(WebSocketSession.class));
        when(sessionStore.deleteByDebateId("1")).thenReturn(Optional.of(session));

        assertThat(adapter.deleteByDebateId("1")).get().isSameAs(session);
    }
}
