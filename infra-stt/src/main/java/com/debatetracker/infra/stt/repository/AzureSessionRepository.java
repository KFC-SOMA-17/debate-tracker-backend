package com.debatetracker.infra.stt.repository;

import com.debatetracker.infra.stt.session.AzureSession;
import java.util.Optional;

public interface AzureSessionRepository {

    void save(AzureSession session);

    Optional<AzureSession> findBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);

    Optional<AzureSession> deleteBySessionId(String sessionId);

}
