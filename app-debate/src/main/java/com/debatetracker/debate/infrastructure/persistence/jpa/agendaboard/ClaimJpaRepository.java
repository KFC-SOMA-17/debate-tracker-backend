package com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface ClaimJpaRepository extends Repository<ClaimEntity, Long> {

    List<ClaimEntity> findByAgendaIdIn(List<Long> agendaIds);

    ClaimEntity save(ClaimEntity entity);
}
