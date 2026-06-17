package com.debatetracker.infra.stt.repository;

import com.debatetracker.infra.stt.router.SttSession;
import com.debatetracker.infra.stt.router.SttSessionRepository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySttSessionRepository implements SttSessionRepository {

    private final ConcurrentHashMap<String, SttSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(String sessionId, SttSession session) {
        sessions.put(sessionId, session);
    }

    @Override
    public Optional<SttSession> findBySessionId(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public boolean existsBySessionId(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    @Override
    public Optional<SttSession> deleteBySessionId(String sessionId) {
        return Optional.ofNullable(sessions.remove(sessionId));
    }
}
