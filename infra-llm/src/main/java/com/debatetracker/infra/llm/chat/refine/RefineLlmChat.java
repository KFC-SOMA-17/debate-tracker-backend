package com.debatetracker.infra.llm.chat.refine;

import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import com.debatetracker.infra.llm.chat.LlmCaller;
import com.debatetracker.infra.llm.chat.LlmChat;
import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RefineLlmChat extends LlmChat<RefineRequest, RefineResponse> {

    private static final RefineLlmChatResponse RESPONSE_EXAMPLE = new RefineLlmChatResponse(
            List.of(new RefineLlmChatSegment("세그먼트 id", "화자 라벨", "정제된 발화")));

    private final ObjectMapper objectMapper;

    public RefineLlmChat(LlmCaller llmCaller, String systemPrompt, String userPrompt,
                         ObjectMapper objectMapper) {
        super(llmCaller, systemPrompt, userPrompt);
        this.objectMapper = objectMapper;
    }

    @Override
    protected String processSystemPrompt(RefineRequest request) {
        return getSystemPrompt()
                .replace("<RESPONSE_JSON_FORMAT>", toJson(RESPONSE_EXAMPLE));
    }

    @Override
    protected String processUserPrompt(RefineRequest request) {
        return getUserPrompt()
                .replace("<CONTEXTS>", toJson(toChatRequest(request.contexts())))
                .replace("<TARGETS>", toJson(toChatRequest(request.targets())));
    }

    @Override
    protected RefineResponse refineResponse(RefineRequest request, String rawResponse) {
        RefineLlmChatResponse response = toObject(rawResponse, RefineLlmChatResponse.class);
        List<TranscriptSegment> segments = response.segments()
                .stream()
                .map(segment -> toTranscriptSegment(segment, request))
                .toList();
        return new RefineResponse(segments);
    }

    private TranscriptSegment toTranscriptSegment(RefineLlmChatSegment responseSegment, RefineRequest request) {
        TranscriptSegment beforeSegment = request.findTargetSegment(responseSegment.id())
                .orElseThrow(() -> {
                    log.warn("[RefineLlmChat] 정제 응답에 요청에 없던 세그먼트 ID 가 포함됨. sessionId={}, 알 수 없는 id={}, 요청 대상 ids={}",
                            request.sessionId(), responseSegment.id(), targetIds(request));
                    return new DebateTrackerException(ErrorCode.REFINE_RESPONSE_SEGMENT_ID_MISMATCH);
                });
        return responseSegment.toTranscriptSegment(beforeSegment.start(), beforeSegment.end());
    }

    @Override
    protected void validate(RefineRequest request, RefineResponse response) {
        List<TranscriptSegment> beforeSegments = request.targets();
        List<TranscriptSegment> afterSegments = response.segments();

        if (beforeSegments.size() != afterSegments.size()) {
            log.warn("[RefineLlmChat] 정제 응답 세그먼트 개수 불일치. sessionId={}, 요청={}건, 응답={}건",
                    request.sessionId(), beforeSegments.size(), afterSegments.size());
            throw new DebateTrackerException(ErrorCode.REFINE_RESPONSE_SEGMENT_SIZE_MISMATCH);
        }
        for (int i = 0; i < beforeSegments.size(); i++) {
            TranscriptSegment beforeSegment = beforeSegments.get(i);
            TranscriptSegment afterSegment = afterSegments.get(i);
            if (!beforeSegment.id().equals(afterSegment.id())) {
                log.warn("[RefineLlmChat] 정제 응답 세그먼트 ID 순서/값 불일치. sessionId={}, index={}, 요청 id={}, 응답 id={}",
                        request.sessionId(), i, beforeSegment.id(), afterSegment.id());
                throw new DebateTrackerException(ErrorCode.REFINE_RESPONSE_SEGMENT_ID_MISMATCH);
            }
        }
    }

    private List<String> targetIds(RefineRequest request) {
        return request.targets()
                .stream()
                .map(TranscriptSegment::id)
                .toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            log.error("[RefineLlmChat] LLM 요청 직렬화 실패. type={}",
                    value == null ? "null" : value.getClass().getSimpleName(),
                    exception);
            throw new DebateTrackerException(ErrorCode.LLM_REQUEST_SERIALIZATION_FAILED, exception);
        }
    }

    private <T> T toObject(String json, Class<T> tClass) {
        try {
            return objectMapper.readValue(json, tClass);
        } catch (JsonProcessingException exception) {
            log.warn("[RefineLlmChat] LLM 응답 파싱 실패. targetType={}, rawResponse=<<<{}>>>", tClass.getSimpleName(), json,
                    exception);
            throw new DebateTrackerException(ErrorCode.LLM_RESPONSE_PARSING_FAILED, exception);
        }
    }

    private RefineLlmChatRequest toChatRequest(List<TranscriptSegment> segments) {
        if (segments == null) {
            return RefineLlmChatRequest.empty();
        }
        return RefineLlmChatRequest.from(segments);
    }
}
