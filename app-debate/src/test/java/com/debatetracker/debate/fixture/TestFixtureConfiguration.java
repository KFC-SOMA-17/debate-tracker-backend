package com.debatetracker.debate.fixture;

import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.AgendaJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.ClaimJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.EvidenceJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateJpaRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Profile({"ci", "local-test"})
@TestConfiguration(proxyBeanMethods = false)
public class TestFixtureConfiguration {

    @Bean
    DebateGenerator debateGenerator(DebateJpaRepository debateJpaRepository) {
        return new DebateGenerator(debateJpaRepository);
    }

    @Bean
    AgendaBoardGenerator agendaBoardGenerator(
            AgendaJpaRepository agendaJpaRepository,
            ClaimJpaRepository claimJpaRepository,
            EvidenceJpaRepository evidenceJpaRepository
    ) {
        return new AgendaBoardGenerator(agendaJpaRepository, claimJpaRepository, evidenceJpaRepository);
    }
}
