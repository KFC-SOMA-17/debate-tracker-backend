package com.debatetracker.infra.llm.chat.refine;

import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import com.debatetracker.infra.llm.chat.LlmChat;
import com.debatetracker.infra.llm.chat.LlmSelector;
import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class RefineLlmChat extends LlmChat<RefineRequest, RefineResponse> {

    private static final RefineLlmChatResponse RESPONSE_EXAMPLE = new RefineLlmChatResponse(
            List.of(new RefineLlmChatSegment("세그먼트 id", "화자 라벨", "정제된 발화")));

    private final ObjectMapper objectMapper;

    public RefineLlmChat(LlmSelector selector, String systemPrompt, String userPrompt, ObjectMapper objectMapper) {
        super(selector, systemPrompt, userPrompt);
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
                .orElseThrow(() -> new DebateTrackerException(ErrorCode.INTERNAL_SERVER_ERROR));
        return responseSegment.toTranscriptSegment(beforeSegment.start(), beforeSegment.end());
    }

    @Override
    protected void validate(RefineRequest request, RefineResponse response) {
        List<TranscriptSegment> beforeSegments = request.targets();
        List<TranscriptSegment> afterSegments = response.segments();

        if (beforeSegments.size() != afterSegments.size()) {
            throw new DebateTrackerException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        for (int i = 0; i < beforeSegments.size(); i++) {
            TranscriptSegment beforeSegment = beforeSegments.get(i);
            TranscriptSegment afterSegment = afterSegments.get(i);
            if (!beforeSegment.id().equals(afterSegment.id())) {
                throw new DebateTrackerException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new DebateTrackerException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private <T> T toObject(String json, Class<T> tClass) {
        try {
            return objectMapper.readValue(json, tClass);
        } catch (JsonProcessingException exception) {
            throw new DebateTrackerException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private RefineLlmChatRequest toChatRequest(List<TranscriptSegment> segments) {
        if (segments == null) {
            return new RefineLlmChatRequest(List.of());
        }
        return RefineLlmChatRequest.from(segments);
    }
}
