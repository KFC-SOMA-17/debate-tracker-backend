package com.debatetracker.debate.infrastructure.persistence.jpa.transcript;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface SpeechBoxJpaRepository extends Repository<SpeechBoxEntity, Long> {

    Optional<SpeechBoxEntity> findFirstByDebateIdOrderByIdDesc(long debateId);

    List<SpeechBoxEntity> findByDebateIdOrderByIdAsc(long debateId);

    SpeechBoxEntity save(SpeechBoxEntity entity);
}
