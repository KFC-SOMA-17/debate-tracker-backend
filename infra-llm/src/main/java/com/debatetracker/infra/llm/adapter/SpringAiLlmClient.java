package com.debatetracker.infra.llm.adapter;

import com.debatetracker.infra.llm.client.LlmClient;
import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import com.debatetracker.infra.llm.client.RefinedSegment;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import com.debatetracker.infra.llm.refine.RefinePrompt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LlmClient} 구현. Spring AI 호출(transport)은 {@link LlmChatCaller} 로 위임하고,
 * 이 클래스는 검증/재시도/폴백/병합 등 기능 로직을 담당한다.
 *
 * <p>id 안전장치: 응답 id 집합이 입력과 다르면 최대 3회까지 재시도하고, 끝내 실패하면 원본을 무손실 반환한다.
 * 검증을 통과해도 id/start/end 는 원본에서 복사하므로 타임스탬프 골격은 구조적으로 보존된다.
 */
@Slf4j
@RequiredArgsConstructor
public class SpringAiLlmClient implements LlmClient {

    private static final int MAX_ATTEMPTS = 3;

    private final LlmChatCaller chatCaller;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    @Override
    public CompletableFuture<RefineResponse> refine(RefineRequest request) {
        return CompletableFuture.supplyAsync(() -> doRefine(request), executor);
    }

    private RefineResponse doRefine(RefineRequest request) {
        List<TranscriptSegment> segments = request.segments();
        if (segments.isEmpty()) {
            return new RefineResponse(segments);
        }

        String userPrompt = RefinePrompt.buildUserPrompt(objectMapper, segments);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String raw = chatCaller.call(RefinePrompt.SYSTEM, userPrompt);
            Optional<Map<String, RefinedSegment>> parsed = parseAndValidate(raw, segments);
            if (parsed.isPresent()) {
                return new RefineResponse(merge(segments, parsed.get()));
            }
            log.warn("정제 응답 검증 실패 (attempt {}/{}), sessionId={}", attempt, MAX_ATTEMPTS, request.sessionId());
        }

        log.warn("정제 {}회 모두 실패 — 원본으로 폴백, sessionId={}", MAX_ATTEMPTS, request.sessionId());
        return new RefineResponse(segments);
    }

    private Optional<Map<String, RefinedSegment>> parseAndValidate(String raw, List<TranscriptSegment> originals) {
        try {
            List<RefinedSegment> items = objectMapper.readValue(stripCodeFence(raw), new TypeReference<>() {
            });
            Map<String, RefinedSegment> byId = new HashMap<>();
            for (RefinedSegment item : items) {
                if (item.id() == null) {
                    return Optional.empty();
                }
                byId.put(item.id(), item);
            }
            Set<String> originalIds = originals.stream()
                    .map(TranscriptSegment::id)
                    .collect(Collectors.toSet());
            if (byId.size() != originalIds.size() || !byId.keySet().equals(originalIds)) {
                return Optional.empty();
            }
            return Optional.of(byId);
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private List<TranscriptSegment> merge(List<TranscriptSegment> originals, Map<String, RefinedSegment> refined) {
        List<TranscriptSegment> result = new ArrayList<>(originals.size());
        for (TranscriptSegment original : originals) {
            RefinedSegment item = refined.get(original.id());
            String text = blankToNull(item.text()) == null ? original.text() : item.text();
            String speaker = blankToNull(item.speaker()) == null ? original.speaker() : item.speaker();
            result.add(new TranscriptSegment(original.id(), speaker, original.start(), original.end(), text));
        }
        return result;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String stripCodeFence(String raw) {
        String trimmed = raw.strip();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline < 0) {
            return trimmed;
        }
        String withoutOpening = trimmed.substring(firstNewline + 1);
        int closingFence = withoutOpening.lastIndexOf("```");
        if (closingFence < 0) {
            return withoutOpening.strip();
        }
        return withoutOpening.substring(0, closingFence).strip();
    }
}
