package com.debatetracker.debate.infrastructure.persistence;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.debate.infrastructure.persistence.inmemory.session.InMemoryDebateSessionRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DebateSessionDomainRepository implements DebateSessionRepository {

    private final InMemoryDebateSessionRepository sessionStore;

    @Override
    public void save(DebateSession session) {
        sessionStore.save(session);
    }

    @Override
    public List<DebateSession> findAll() {
        return sessionStore.findAll();
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
