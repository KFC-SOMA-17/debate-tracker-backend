package com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface AgendaJpaRepository extends Repository<AgendaEntity, Long> {

    List<AgendaEntity> findByDebateId(long debateId);

    AgendaEntity save(AgendaEntity entity);
}
