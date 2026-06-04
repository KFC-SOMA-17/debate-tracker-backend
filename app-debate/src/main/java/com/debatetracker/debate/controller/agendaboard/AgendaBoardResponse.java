package com.debatetracker.debate.controller.agendaboard;

import com.debatetracker.debate.domain.agendaboard.Agenda;
import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.domain.agendaboard.Claim;
import com.debatetracker.debate.domain.agendaboard.Evidence;
import com.debatetracker.debate.domain.agendaboard.EvidenceType;
import com.debatetracker.debate.domain.agendaboard.Stance;
import java.time.LocalDateTime;
import java.util.List;

public record AgendaBoardResponse(Long debateId, List<AgendaResponse> agendas) {

    public static AgendaBoardResponse from(AgendaBoard agendaBoard) {
        List<AgendaResponse> agendas = agendaBoard.getAgendas().stream()
                .map(AgendaResponse::from)
                .toList();
        return new AgendaBoardResponse(agendaBoard.getDebateId(), agendas);
    }

    public record AgendaResponse(Long agendaId, String content, LocalDateTime createdAt, LocalDateTime modifiedAt,
                                 List<ClaimResponse> claims) {

        public static AgendaResponse from(Agenda agenda) {
            List<ClaimResponse> claims = agenda.getClaims().stream()
                    .map(ClaimResponse::from)
                    .toList();
            return new AgendaResponse(agenda.getId(), agenda.getContent(),
                    agenda.getCreatedAt(), agenda.getModifiedAt(), claims);
        }
    }

    public record ClaimResponse(Long claimId, String content, Stance stance, LocalDateTime createdAt,
                                LocalDateTime modifiedAt, List<EvidenceResponse> evidences) {

        public static ClaimResponse from(Claim claim) {
            List<EvidenceResponse> evidences = claim.getEvidences().stream()
                    .map(EvidenceResponse::from)
                    .toList();
            return new ClaimResponse(claim.getId(), claim.getContent(), claim.getStance(),
                    claim.getCreatedAt(), claim.getModifiedAt(), evidences);
        }
    }

    public record EvidenceResponse(Long evidenceId, String content, EvidenceType type, LocalDateTime createdAt,
                                   LocalDateTime modifiedAt) {

        public static EvidenceResponse from(Evidence evidence) {
            return new EvidenceResponse(evidence.getId(), evidence.getContent(), evidence.getType(),
                    evidence.getCreatedAt(), evidence.getModifiedAt());
        }
    }
}
