package com.debatetracker.debate.fixture;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateJpaRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DebateGenerator {

    private final DebateJpaRepository debateJpaRepository;

    public Debate generate(String topic) {
        DebateEntity saved = debateJpaRepository.save(new DebateEntity(new Debate(null, topic)));
        return saved.toDomain();
    }
}
