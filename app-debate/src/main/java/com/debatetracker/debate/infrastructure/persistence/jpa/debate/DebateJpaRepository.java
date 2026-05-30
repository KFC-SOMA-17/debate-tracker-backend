package com.debatetracker.debate.infrastructure.persistence.jpa.debate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DebateJpaRepository extends JpaRepository<DebateEntity, Long> {
}
