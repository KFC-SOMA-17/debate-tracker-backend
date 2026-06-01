package com.debatetracker.debate.infrastructure.persistence.jpa.debate;

import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface DebateJpaRepository extends Repository<DebateEntity, Long> {

    Optional<DebateEntity> findById(Long id);

    DebateEntity save(DebateEntity entity);
}
