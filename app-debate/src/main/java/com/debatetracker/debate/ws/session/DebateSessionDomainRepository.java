package com.debatetracker.debate.ws.session;

import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * DebateSessionRepository 포트의 어댑터. 현재는 in-memory 저장소에 위임한다.
 * (DebateDomainRepository 가 JPA repo 를 감싸는 것과 동일한 자리.)
 */
@Primary
@Component
public class DebateSessionDomainRepository implements DebateSessionRepository {

    private final InMemoryDebateSessionRepository sessionStore;

    public DebateSessionDomainRepository(InMemoryDebateSessionRepository sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public void save(DebateSession session) {
        sessionStore.save(session);
    }

    @Override
    public Optional<DebateSession> findByDebateId(String debateId) {
        return sessionStore.findByDebateId(debateId);
    }

    @Override
    public boolean existsByDebateId(String debateId) {
        return sessionStore.existsByDebateId(debateId);
    }

    @Override
    public Optional<DebateSession> deleteByDebateId(String debateId) {
        return sessionStore.deleteByDebateId(debateId);
    }
}
