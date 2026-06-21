package com.debatetracker.infra.stt.router;

import java.util.Optional;

public interface SttSessionRepository {

    void save(String sessionId, SttSession session);

    Optional<SttSession> findBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);

    Optional<SttSession> deleteBySessionId(String sessionId);

    int count();
}
