package com.debatetracker.debate.ws.session;

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
