package com.debatetracker.debate.infrastructure.persistence;

import com.debatetracker.debate.domain.agendaboard.Agenda;
import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.domain.agendaboard.Claim;
import com.debatetracker.debate.domain.agendaboard.Evidence;
import com.debatetracker.debate.domain.agendaboard.repository.AgendaBoardRepository;
import com.debatetracker.debate.infrastructure.persistence.jdbc.agendaboard.AgendaJdbcRepository;
import com.debatetracker.debate.infrastructure.persistence.jdbc.agendaboard.ClaimJdbcRepository;
import com.debatetracker.debate.infrastructure.persistence.jdbc.agendaboard.EvidenceJdbcRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.AgendaEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.AgendaJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.ClaimEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.ClaimJpaRepository;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.EvidenceEntity;
import com.debatetracker.debate.infrastructure.persistence.jpa.agendaboard.EvidenceJpaRepository;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final AgendaJdbcRepository agendaJdbcRepository;
    private final ClaimJdbcRepository claimJdbcRepository;
    private final EvidenceJdbcRepository evidenceJdbcRepository;

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
    public AgendaBoard upsert(AgendaBoard agendaBoard) {
        List<Agenda> agendasWithId = upsertAgendas(agendaBoard.getAgendas());
        List<Claim> claimsWithId = upsertClaims(stampAgendaId(agendasWithId));
        List<Evidence> evidencesWithId = upsertEvidences(stampClaimId(claimsWithId));
        return new AgendaBoard(agendaBoard.getDebateId(), rebuild(agendasWithId, claimsWithId, evidencesWithId));
    }

    private List<Agenda> upsertAgendas(List<Agenda> agendas) {
        List<Agenda> existing = agendas.stream().filter(agenda -> agenda.getId() != null).toList();
        List<Agenda> created = agendas.stream().filter(agenda -> agenda.getId() == null).toList();
        agendaJdbcRepository.batchUpdate(existing);
        Iterator<Long> newIds = agendaJdbcRepository.batchInsert(created).iterator();
        return agendas.stream()
                .map(agenda -> Optional.ofNullable(agenda.getId())
                        .map(id -> agenda)
                        .orElseGet(() -> new Agenda(newIds.next(), agenda.getDebateId(), agenda.getContent(),
                                agenda.getCreatedAt(), agenda.getModifiedAt(), agenda.getClaims())))
                .toList();
    }

    private List<Claim> upsertClaims(List<Claim> claims) {
        List<Claim> existing = claims.stream().filter(claim -> claim.getId() != null).toList();
        List<Claim> created = claims.stream().filter(claim -> claim.getId() == null).toList();
        claimJdbcRepository.batchUpdate(existing);
        Iterator<Long> newIds = claimJdbcRepository.batchInsert(created).iterator();
        return claims.stream()
                .map(claim -> Optional.ofNullable(claim.getId())
                        .map(id -> claim)
                        .orElseGet(() -> new Claim(newIds.next(), claim.getAgendaId(), claim.getContent(),
                                claim.getStance(), claim.getCreatedAt(), claim.getModifiedAt(), claim.getEvidences())))
                .toList();
    }

    private List<Evidence> upsertEvidences(List<Evidence> evidences) {
        List<Evidence> existing = evidences.stream().filter(evidence -> evidence.getId() != null).toList();
        List<Evidence> created = evidences.stream().filter(evidence -> evidence.getId() == null).toList();
        evidenceJdbcRepository.batchUpdate(existing);
        Iterator<Long> newIds = evidenceJdbcRepository.batchInsert(created).iterator();
        return evidences.stream()
                .map(evidence -> Optional.ofNullable(evidence.getId())
                        .map(id -> evidence)
                        .orElseGet(() -> new Evidence(newIds.next(), evidence.getClaimId(), evidence.getContent(),
                                evidence.getType(), evidence.getCreatedAt(), evidence.getModifiedAt())))
                .toList();
    }

    private List<Claim> stampAgendaId(List<Agenda> agendas) {
        List<Claim> claims = new ArrayList<>();
        for (Agenda agenda : agendas) {
            for (Claim claim : agenda.getClaims()) {
                claims.add(new Claim(claim.getId(), agenda.getId(), claim.getContent(), claim.getStance(),
                        claim.getCreatedAt(), claim.getModifiedAt(), claim.getEvidences()));
            }
        }
        return claims;
    }

    private List<Evidence> stampClaimId(List<Claim> claims) {
        List<Evidence> evidences = new ArrayList<>();
        for (Claim claim : claims) {
            for (Evidence evidence : claim.getEvidences()) {
                evidences.add(new Evidence(evidence.getId(), claim.getId(), evidence.getContent(),
                        evidence.getType(), evidence.getCreatedAt(), evidence.getModifiedAt()));
            }
        }
        return evidences;
    }

    private List<Agenda> rebuild(List<Agenda> agendas, List<Claim> claims, List<Evidence> evidences) {
        Map<Long, List<Evidence>> evidencesByClaimId = evidences.stream()
                .collect(Collectors.groupingBy(Evidence::getClaimId));
        Map<Long, List<Claim>> claimsByAgendaId = claims.stream()
                .collect(Collectors.groupingBy(
                        Claim::getAgendaId,
                        Collectors.mapping(claim -> new Claim(claim.getId(), claim.getAgendaId(), claim.getContent(),
                                claim.getStance(), claim.getCreatedAt(), claim.getModifiedAt(),
                                evidencesByClaimId.getOrDefault(claim.getId(), List.of())), Collectors.toList())));
        return agendas.stream()
                .map(agenda -> new Agenda(agenda.getId(), agenda.getDebateId(), agenda.getContent(),
                        agenda.getCreatedAt(), agenda.getModifiedAt(),
                        claimsByAgendaId.getOrDefault(agenda.getId(), List.of())))
                .toList();
    }

    private Claim toClaim(ClaimEntity claim, Map<Long, List<EvidenceEntity>> evidencesByClaimId) {
        List<Evidence> evidences = evidencesByClaimId.getOrDefault(claim.getId(), List.of()).stream()
                .map(EvidenceEntity::toDomain)
                .toList();
        return claim.toDomain(evidences);
    }
}
