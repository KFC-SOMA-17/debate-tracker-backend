package com.debatetracker.debate.infrastructure.persistence;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.domain.debate.DebateRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.redis.DebateMetaRedisCache;
import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebateDomainRepository implements DebateRepository {

    private final DebateJpaRepository debateJpaRepository;
    private final DebateMetaRedisCache debateMetaRedisCache;

    @Override
    public Debate create(Debate debate) {
        DebateEntity savedEntity = debateJpaRepository.save(new DebateEntity(debate));
        Debate saved = savedEntity.toDomain();
        debateMetaRedisCache.save(saved);
        return saved;
    }

    @Override
    public Debate findById(Long id) {
        return debateMetaRedisCache.find(id)
                .or(() -> findFromDatabase(id))
                .orElseThrow(() -> {
                    log.error("토론을 찾을 수 없습니다: debateId={}", id);
                    return new DebateTrackerException(ErrorCode.NOT_FOUND_DEBATE_ID);
                });
    }

    private Optional<Debate> findFromDatabase(Long id) {
        Optional<Debate> found = debateJpaRepository.findById(id).map(DebateEntity::toDomain);
        found.ifPresent(debateMetaRedisCache::saveAsync);
        return found;
    }
}
