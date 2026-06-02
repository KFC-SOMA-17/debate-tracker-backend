package com.debatetracker.infra.stt.repository;

import com.debatetracker.infra.stt.session.AzureTranscriberSession;
import java.util.Optional;

public interface AzureTranscriberSessionRepository {

    void save(AzureTranscriberSession session);

    Optional<AzureTranscriberSession> findBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);

    Optional<AzureTranscriberSession> deleteBySessionId(String sessionId);

}
