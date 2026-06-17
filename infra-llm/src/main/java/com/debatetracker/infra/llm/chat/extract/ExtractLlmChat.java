package com.debatetracker.infra.llm.chat.extract;

import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import com.debatetracker.infra.llm.chat.LlmCaller;
import com.debatetracker.infra.llm.chat.LlmChat;
import com.debatetracker.infra.llm.client.ExtractAgenda;
import com.debatetracker.infra.llm.client.ExtractAgendaRequest;
import com.debatetracker.infra.llm.client.ExtractAgendaResponse;
import com.debatetracker.infra.llm.client.ExtractClaim;
import com.debatetracker.infra.llm.client.ExtractEvidenceType;
import com.debatetracker.infra.llm.client.ExtractStance;
import com.debatetracker.infra.llm.client.ExtractedEvidence;
import com.debatetracker.serdes.JsonUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExtractLlmChat extends LlmChat<ExtractAgendaRequest, ExtractAgendaResponse> {

    private static final ExtractAgendaResponse RESPONSE_EXAMPLE = new ExtractAgendaResponse(List.of(
            new ExtractAgenda(null, "양심적 병역거부", List.of(
                    new ExtractClaim(null, ExtractStance.PROS, "양심·종교의 자유 침해", List.of(
                            new ExtractedEvidence(null, ExtractEvidenceType.STATISTICS,
                                    "유엔 인권이사회 - 전세계 병역거부 수감자 92%가 한국인"))),
                    new ExtractClaim(null, ExtractStance.CONS, "대체복무로 해결 가능", List.of()))),
            new ExtractAgenda(null, "모병제 전환의 경제성", List.of(
                    new ExtractClaim(null, ExtractStance.PROS, "국방예산 절감", List.of(
                            new ExtractedEvidence(null, ExtractEvidenceType.QUOTATION,
                                    "진호용 준장 - 육군 30만 감축 시 5년 유지비로 군인 월급 충당 가능"))),
                    new ExtractClaim(null, ExtractStance.CONS, "전환비용으로 국방비 부담 가중", List.of(
                            new ExtractedEvidence(null, ExtractEvidenceType.EXAMPLE,
                                    "러시아 - 모병제 전환 후 국방비 31.2% 증가")))))));

    public ExtractLlmChat(LlmCaller llmCaller, String systemPrompt, String userPrompt) {
        super(llmCaller, systemPrompt, userPrompt);
    }

    @Override
    protected String processSystemPrompt(ExtractAgendaRequest request) {
        return getSystemPrompt()
                .replace("<RESPONSE_JSON_FORMAT>", JsonUtils.serialize(RESPONSE_EXAMPLE))
                .replace("<STANCE_VALUES>", enumValues(ExtractStance.values()))
                .replace("<EVIDENCE_TYPE_VALUES>", enumValues(ExtractEvidenceType.values()));
    }

    @Override
    protected String processUserPrompt(ExtractAgendaRequest request) {
        return getUserPrompt()
                .replace("<CONTEXTS>", JsonUtils.serialize(orEmpty(request.contexts())))
                .replace("<BEFORE_AGENDAS>", JsonUtils.serialize(orEmpty(request.beforeAgendas())));
    }

    @Override
    protected ExtractAgendaResponse refineResponse(ExtractAgendaRequest request, String rawResponse) {
        return JsonUtils.deserialize(rawResponse, ExtractAgendaResponse.class);
    }

    @Override
    protected void validate(ExtractAgendaRequest request, ExtractAgendaResponse response) {
        if (response.agendas() == null) {
            throwInvalidFormat(request, "agendas 가 null");
        }
        for (ExtractAgenda agenda : response.agendas()) {
            validateAgenda(request, agenda);
        }
    }

    private void validateAgenda(ExtractAgendaRequest request, ExtractAgenda agenda) {
        if (isBlank(agenda.content())) {
            throwInvalidFormat(request, "쟁점 content 가 비어 있음");
        }
        if (agenda.claims() == null) {
            throwInvalidFormat(request, "쟁점의 주장 목록이 null");
        }
        for (ExtractClaim claim : agenda.claims()) {
            validateClaim(request, claim);
        }
    }

    private void validateClaim(ExtractAgendaRequest request, ExtractClaim claim) {
        if (claim.stance() == null) {
            throwInvalidFormat(request, "주장 stance 가 null");
        }
        if (isBlank(claim.content())) {
            throwInvalidFormat(request, "주장 content 가 비어 있음");
        }
        if (claim.evidences() == null) {
            throwInvalidFormat(request, "주장의 근거 목록이 null");
        }
        for (ExtractedEvidence evidence : claim.evidences()) {
            validateEvidence(request, evidence);
        }
    }

    private void validateEvidence(ExtractAgendaRequest request, ExtractedEvidence evidence) {
        if (evidence.type() == null) {
            throwInvalidFormat(request, "근거 type 이 null");
        }
        if (isBlank(evidence.content())) {
            throwInvalidFormat(request, "근거 content 가 비어 있음");
        }
    }

    private void throwInvalidFormat(ExtractAgendaRequest request, String reason) {
        log.warn("[ExtractLlmChat] 쟁점 추출 응답 형식 오류. sessionId={}, 원인={}", request.sessionId(), reason);
        throw new DebateTrackerException(ErrorCode.EXTRACT_RESPONSE_INVALID_FORMAT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<?> orEmpty(List<?> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    private String enumValues(Enum<?>[] values) {
        return Arrays.stream(values)
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
