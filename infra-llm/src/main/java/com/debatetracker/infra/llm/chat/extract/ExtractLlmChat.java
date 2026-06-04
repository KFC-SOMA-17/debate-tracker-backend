package com.debatetracker.infra.llm.chat.extract;

import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import com.debatetracker.infra.llm.chat.LlmChat;
import com.debatetracker.infra.llm.chat.LlmSelector;
import com.debatetracker.infra.llm.client.ExtractAgenda;
import com.debatetracker.infra.llm.client.ExtractAgendaRequest;
import com.debatetracker.infra.llm.client.ExtractAgendaResponse;
import com.debatetracker.infra.llm.client.ExtractClaim;
import com.debatetracker.infra.llm.client.ExtractEvidenceType;
import com.debatetracker.infra.llm.client.ExtractStance;
import com.debatetracker.infra.llm.client.ExtractedEvidence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExtractLlmChat extends LlmChat<ExtractAgendaRequest, ExtractAgendaResponse> {

    private static final ExtractAgendaResponse RESPONSE_EXAMPLE = new ExtractAgendaResponse(List.of(
            new ExtractAgenda("한 문장으로 진술된 쟁점 예시", List.of(
                    new ExtractClaim(ExtractStance.PROS, "찬성 측 핵심 주장 예시", List.of(
                            new ExtractedEvidence(ExtractEvidenceType.STATISTICS, "근거 예시"))),
                    new ExtractClaim(ExtractStance.CONS, "반대 측 핵심 주장 예시", List.of(
                            new ExtractedEvidence(ExtractEvidenceType.EXAMPLE, "근거 예시")))))));

    private final ObjectMapper objectMapper;

    public ExtractLlmChat(LlmSelector selector, String systemPrompt, String userPrompt, ObjectMapper objectMapper) {
        super(selector, systemPrompt, userPrompt);
        this.objectMapper = objectMapper;
    }

    @Override
    protected String processSystemPrompt(ExtractAgendaRequest request) {
        return getSystemPrompt()
                .replace("<RESPONSE_JSON_FORMAT>", toJson(RESPONSE_EXAMPLE))
                .replace("<STANCE_VALUES>", enumValues(ExtractStance.values()))
                .replace("<EVIDENCE_TYPE_VALUES>", enumValues(ExtractEvidenceType.values()));
    }

    @Override
    protected String processUserPrompt(ExtractAgendaRequest request) {
        return getUserPrompt()
                .replace("<CONTEXTS>", toJson(orEmpty(request.contexts())))
                .replace("<BEFORE_AGENDAS>", toJson(orEmpty(request.beforeAgendas())));
    }

    @Override
    protected ExtractAgendaResponse refineResponse(ExtractAgendaRequest request, String rawResponse) {
        return toObject(rawResponse, ExtractAgendaResponse.class);
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            log.error("[ExtractLlmChat] LLM 요청 직렬화 실패. type={}",
                    value == null ? "null" : value.getClass().getSimpleName(),
                    exception);
            throw new DebateTrackerException(ErrorCode.LLM_REQUEST_SERIALIZATION_FAILED, exception);
        }
    }

    private <T> T toObject(String json, Class<T> tClass) {
        try {
            return objectMapper.readValue(json, tClass);
        } catch (JsonProcessingException exception) {
            log.warn("[ExtractLlmChat] LLM 응답 파싱 실패. targetType={}, rawResponse=<<<{}>>>", tClass.getSimpleName(), json,
                    exception);
            throw new DebateTrackerException(ErrorCode.LLM_RESPONSE_PARSING_FAILED, exception);
        }
    }
}
