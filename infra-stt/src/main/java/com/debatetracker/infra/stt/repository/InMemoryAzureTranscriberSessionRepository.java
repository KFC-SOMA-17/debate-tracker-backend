package com.debatetracker.infra.stt.repository;

import com.debatetracker.infra.stt.session.AzureTranscriberSession;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAzureTranscriberSessionRepository implements AzureTranscriberSessionRepository {

    private final ConcurrentHashMap<String, AzureTranscriberSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(AzureTranscriberSession session) {
        sessions.put(session.sessionId(), session);
    }

    @Override
    public Optional<AzureTranscriberSession> findBySessionId(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public boolean existsBySessionId(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    @Override
    public Optional<AzureTranscriberSession> deleteBySessionId(String sessionId) {
        return Optional.ofNullable(sessions.remove(sessionId));
    }
}
