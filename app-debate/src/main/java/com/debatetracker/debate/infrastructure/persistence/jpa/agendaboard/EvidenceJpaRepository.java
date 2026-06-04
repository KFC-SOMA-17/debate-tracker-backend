package com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface EvidenceJpaRepository extends Repository<EvidenceEntity, Long> {

    List<EvidenceEntity> findByClaimIdIn(List<Long> claimIds);

    EvidenceEntity save(EvidenceEntity entity);
}
