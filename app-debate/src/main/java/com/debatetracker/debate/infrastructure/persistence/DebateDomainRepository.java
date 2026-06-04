package com.debatetracker.debate.infrastructure.persistence;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.domain.debate.DebateRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateJpaRepository;
import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebateDomainRepository implements DebateRepository {

    private final DebateJpaRepository debateJpaRepository;

    @Override
    public Debate create(Debate debate) {
        DebateEntity savedEntity = debateJpaRepository.save(new DebateEntity(debate));
        return savedEntity.toDomain();
    }

    @Override
    public Debate findById(Long id) {
        return debateJpaRepository.findById(id)
                .map(DebateEntity::toDomain)
                .orElseThrow(() -> {
                    log.error("토론을 찾을 수 없습니다: debateId={}", id);
                    return new DebateTrackerException(ErrorCode.NOT_FOUND_DEBATE_ID);
                });
    }
}
