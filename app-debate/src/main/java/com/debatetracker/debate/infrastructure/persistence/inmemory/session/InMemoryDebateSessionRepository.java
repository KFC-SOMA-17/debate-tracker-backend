package com.debatetracker.debate.infrastructure.persistence.inmemory.session;

import com.debatetracker.debate.domain.session.DebateSession;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryDebateSessionRepository {

    private final ConcurrentHashMap<String, DebateSession> sessions = new ConcurrentHashMap<>();

    public void save(DebateSession session) {
        sessions.put(session.debateId(), session);
    }

    public List<DebateSession> findAll() {
        return List.copyOf(sessions.values());
    }

    public Optional<DebateSession> findByDebateId(String debateId) {
        return Optional.ofNullable(sessions.get(debateId));
    }

    public boolean existsByDebateId(String debateId) {
        return sessions.containsKey(debateId);
    }

    public Optional<DebateSession> deleteByDebateId(String debateId) {
        return Optional.ofNullable(sessions.remove(debateId));
    }
}
