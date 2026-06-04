package com.debatetracker.debate.infrastructure.persistence;

import com.debatetracker.debate.domain.agendaboard.Agenda;
import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.domain.agendaboard.Claim;
import com.debatetracker.debate.domain.agendaboard.Evidence;
import com.debatetracker.debate.domain.agendaboard.repository.AgendaBoardRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.AgendaEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.AgendaJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.ClaimEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.ClaimJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.EvidenceEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.EvidenceJpaRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgendaBoardDomainRepository implements AgendaBoardRepository {

    private final AgendaJpaRepository agendaJpaRepository;
    private final ClaimJpaRepository claimJpaRepository;
    private final EvidenceJpaRepository evidenceJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public AgendaBoard findByDebateId(long debateId) {
        List<AgendaEntity> agendaEntities = agendaJpaRepository.findByDebateId(debateId);
        List<Long> agendaIds = agendaEntities.stream()
                .map(AgendaEntity::getId)
                .toList();

        List<ClaimEntity> claimEntities = claimJpaRepository.findByAgendaIdIn(agendaIds);
        List<Long> claimIds = claimEntities.stream()
                .map(ClaimEntity::getId)
                .toList();

        Map<Long, List<EvidenceEntity>> evidencesByClaimId = evidenceJpaRepository.findByClaimIdIn(claimIds).stream()
                .collect(Collectors.groupingBy(EvidenceEntity::getClaimId));
        Map<Long, List<Claim>> claimsByAgendaId = claimEntities.stream()
                .collect(Collectors.groupingBy(
                        ClaimEntity::getAgendaId,
                        Collectors.mapping(claim -> toClaim(claim, evidencesByClaimId), Collectors.toList())));

        List<Agenda> agendas = agendaEntities.stream()
                .map(agenda -> agenda.toDomain(claimsByAgendaId.getOrDefault(agenda.getId(), List.of())))
                .toList();
        return new AgendaBoard(debateId, agendas);
    }

    @Override
    @Transactional
    public AgendaBoard save(AgendaBoard agendaBoard) {
        // TODO : Batch Insert/Update Query
        List<Agenda> savedAgendas = agendaBoard.getAgendas().stream()
                .map(this::saveAgenda)
                .toList();
        return new AgendaBoard(agendaBoard.getDebateId(), savedAgendas);
    }

    private Claim toClaim(ClaimEntity claim, Map<Long, List<EvidenceEntity>> evidencesByClaimId) {
        List<Evidence> evidences = evidencesByClaimId.getOrDefault(claim.getId(), List.of()).stream()
                .map(EvidenceEntity::toDomain)
                .toList();
        return claim.toDomain(evidences);
    }

    private Agenda saveAgenda(Agenda agenda) {
        AgendaEntity savedAgenda = agendaJpaRepository.save(new AgendaEntity(agenda));
        List<Claim> savedClaims = agenda.getClaims().stream()
                .map(this::saveClaim)
                .toList();
        return savedAgenda.toDomain(savedClaims);
    }

    private Claim saveClaim(Claim claim) {
        ClaimEntity savedClaim = claimJpaRepository.save(new ClaimEntity(claim));
        List<Evidence> savedEvidences = claim.getEvidences().stream()
                .map(this::saveEvidence)
                .toList();
        return savedClaim.toDomain(savedEvidences);
    }

    private Evidence saveEvidence(Evidence evidence) {
        return evidenceJpaRepository.save(new EvidenceEntity(evidence)).toDomain();
    }
}
