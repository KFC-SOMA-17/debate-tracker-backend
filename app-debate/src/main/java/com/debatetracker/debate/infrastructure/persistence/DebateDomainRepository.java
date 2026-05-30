package com.debatetracker.debate.infrastructure.persistence;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.domain.debate.DebateRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.transcript.DebateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DebateDomainRepository implements DebateRepository {

    private final DebateJpaRepository debateJpaRepository;

    @Override
    public Debate create(Debate debate) {
        DebateEntity savedEntity = debateJpaRepository.save(new DebateEntity(debate));
        return savedEntity.toDomain();
    }
}
