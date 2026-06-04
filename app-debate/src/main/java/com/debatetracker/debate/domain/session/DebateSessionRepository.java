package com.debatetracker.debate.domain.session;

import java.util.List;
import java.util.Optional;

public interface DebateSessionRepository {

    void save(DebateSession session);

    List<DebateSession> findAll();

    Optional<DebateSession> findByDebateId(String debateId);

    boolean existsByDebateId(String debateId);

    Optional<DebateSession> deleteByDebateId(String debateId);
}
