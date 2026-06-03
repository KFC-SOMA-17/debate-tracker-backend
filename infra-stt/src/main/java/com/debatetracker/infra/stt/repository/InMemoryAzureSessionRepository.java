package com.debatetracker.infra.stt.repository;

import com.debatetracker.infra.stt.session.AzureSession;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAzureSessionRepository implements AzureSessionRepository {

    private final ConcurrentHashMap<String, AzureSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(AzureSession session) {
        sessions.put(session.sessionId(), session);
    }

    @Override
    public Optional<AzureSession> findBySessionId(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public boolean existsBySessionId(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    @Override
    public Optional<AzureSession> deleteBySessionId(String sessionId) {
        return Optional.ofNullable(sessions.remove(sessionId));
    }
}
