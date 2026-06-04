package com.debatetracker.debate.fixture;

import com.debatetracker.debate.domain.agendaboard.Agenda;
import com.debatetracker.debate.domain.agendaboard.Claim;
import com.debatetracker.debate.domain.agendaboard.Evidence;
import com.debatetracker.debate.domain.agendaboard.EvidenceType;
import com.debatetracker.debate.domain.agendaboard.Stance;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.AgendaEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.AgendaJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.ClaimEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.ClaimJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.EvidenceEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.EvidenceJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AgendaBoardGenerator {

    private final AgendaJpaRepository agendaJpaRepository;
    private final ClaimJpaRepository claimJpaRepository;
    private final EvidenceJpaRepository evidenceJpaRepository;

    public Agenda generateAgenda(long debateId, String content) {
        AgendaEntity saved = agendaJpaRepository.save(
                new AgendaEntity(new Agenda(null, debateId, content, null, null, List.of())));
        return saved.toDomain(List.of());
    }

    public Claim generateClaim(long agendaId, String content, Stance stance) {
        ClaimEntity saved = claimJpaRepository.save(
                new ClaimEntity(new Claim(null, agendaId, content, stance, null, null, List.of())));
        return saved.toDomain(List.of());
    }

    public Evidence generateEvidence(long claimId, String content, EvidenceType type) {
        EvidenceEntity saved = evidenceJpaRepository.save(
                new EvidenceEntity(new Evidence(null, claimId, content, type, null, null)));
        return saved.toDomain();
    }
}
