package com.debatetracker.debate.client;

import com.debatetracker.debate.domain.agendaboard.Agenda;
import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.domain.agendaboard.Claim;
import com.debatetracker.debate.domain.agendaboard.Evidence;
import com.debatetracker.debate.domain.agendaboard.EvidenceType;
import com.debatetracker.debate.domain.agendaboard.Stance;
import com.debatetracker.debate.domain.transcript.SpeechBox;
import com.debatetracker.debate.service.agendaboard.DebateAgendaAnalyzer;
import com.debatetracker.infra.llm.client.ExtractAgenda;
import com.debatetracker.infra.llm.client.ExtractAgendaRequest;
import com.debatetracker.infra.llm.client.ExtractAgendaResponse;
import com.debatetracker.infra.llm.client.ExtractClaim;
import com.debatetracker.infra.llm.client.ExtractEvidenceType;
import com.debatetracker.infra.llm.client.ExtractStance;
import com.debatetracker.infra.llm.client.ExtractedEvidence;
import com.debatetracker.infra.llm.client.LlmClient;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "llm.mode", havingValue = "real")
public class LlmDebateAgendaAnalyzerAdapter implements DebateAgendaAnalyzer {

    private final LlmClient llmClient;

    @Override
    public AgendaBoard analyze(long debateId, List<SpeechBox> speeches, AgendaBoard beforeBoard) {
        log.debug("LLM 쟁점 추출: debateId={}, speeches={}건, agendas={}건",
                debateId, speeches.size(), beforeBoard.getAgendas().size());
        ExtractAgendaRequest request = new ExtractAgendaRequest(
                String.valueOf(debateId), toContexts(speeches), toExtractAgendas(beforeBoard.getAgendas()));
        ExtractAgendaResponse response = llmClient.extract(request);
        return toAgendaBoard(debateId, response.agendas());
    }

    private List<TranscriptSegment> toContexts(List<SpeechBox> speeches) {
        return speeches.stream()
                .map(box -> new TranscriptSegment(
                        String.valueOf(box.getId()),
                        box.getSpeaker(),
                        box.getStartAt(),
                        box.getEndAt(),
                        box.getContent()))
                .toList();
    }

    private List<ExtractAgenda> toExtractAgendas(List<Agenda> agendas) {
        return agendas.stream()
                .map(agenda -> new ExtractAgenda(
                        toStringId(agenda.getId()),
                        agenda.getContent(),
                        toExtractClaims(agenda.getClaims())))
                .toList();
    }

    private List<ExtractClaim> toExtractClaims(List<Claim> claims) {
        return claims.stream()
                .map(claim -> new ExtractClaim(
                        toStringId(claim.getId()),
                        ExtractStance.valueOf(claim.getStance().name()),
                        claim.getContent(),
                        toExtractEvidences(claim.getEvidences())))
                .toList();
    }

    private List<ExtractedEvidence> toExtractEvidences(List<Evidence> evidences) {
        return evidences.stream()
                .map(evidence -> new ExtractedEvidence(
                        toStringId(evidence.getId()),
                        ExtractEvidenceType.valueOf(evidence.getType().name()),
                        evidence.getContent()))
                .toList();
    }

    private AgendaBoard toAgendaBoard(long debateId, List<ExtractAgenda> agendas) {
        List<Agenda> domainAgendas = agendas.stream()
                .map(agenda -> new Agenda(
                        toLongId(agenda.id()),
                        debateId,
                        agenda.content(),
                        null,
                        null,
                        toClaims(agenda.claims())))
                .toList();
        return new AgendaBoard(debateId, domainAgendas);
    }

    private List<Claim> toClaims(List<ExtractClaim> claims) {
        return claims.stream()
                .map(claim -> new Claim(
                        toLongId(claim.id()),
                        0L,
                        claim.content(),
                        Stance.valueOf(claim.stance().name()),
                        null,
                        null,
                        toEvidences(claim.evidences())))
                .toList();
    }

    private List<Evidence> toEvidences(List<ExtractedEvidence> evidences) {
        return evidences.stream()
                .map(evidence -> new Evidence(
                        toLongId(evidence.id()),
                        0L,
                        evidence.content(),
                        EvidenceType.valueOf(evidence.type().name()),
                        null,
                        null))
                .toList();
    }

    private String toStringId(Long id) {
        return Optional.ofNullable(id).map(String::valueOf).orElse(null);
    }

    private Long toLongId(String id) {
        return Optional.ofNullable(id).map(Long::parseLong).orElse(null);
    }
}
