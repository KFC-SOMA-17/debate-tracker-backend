package com.debatetracker.debate.infrastructure.persistence.inmemory.session;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryDebateSessionRepository implements DebateSessionRepository {

    private final ConcurrentHashMap<String, DebateSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(DebateSession session) {
        sessions.put(session.debateId(), session);
    }

    @Override
    public List<DebateSession> findAll() {
        return List.copyOf(sessions.values());
    }

    @Override
    public Optional<DebateSession> findByDebateId(String debateId) {
        return Optional.ofNullable(sessions.get(debateId));
    }

    @Override
    public boolean existsByDebateId(String debateId) {
        return sessions.containsKey(debateId);
    }

    @Override
    public Optional<DebateSession> deleteByDebateId(String debateId) {
        return Optional.ofNullable(sessions.remove(debateId));
    }
}
