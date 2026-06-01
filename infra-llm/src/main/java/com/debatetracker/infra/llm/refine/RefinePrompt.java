package com.debatetracker.infra.llm.refine;

import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 * 세그먼트 보정 프롬프트 빌더. 목표: 문맥 기반 정제 + 최대한 빠른 응답.
 */
public class RefinePrompt {

    public static final String SYSTEM = """
            너는 한국어 토론 음성 STT 결과를 후처리하는 정제기다.
            입력은 발화 세그먼트 배열이며, 각 세그먼트는 id, speaker, text 를 가진다.

            반드시 지킬 규칙(불변식):
            - 세그먼트를 추가/삭제/병합/분할하지 마라. 입력과 출력의 개수와 id 집합은 완전히 동일해야 한다.
            - 각 세그먼트의 id 는 그대로 둔다.
            - 발화에 없는 내용을 새로 생성하지 마라.

            허용 작업:
            - 조각난 단어를 인접 세그먼트의 문맥으로 완성하거나 알맞은 세그먼트로 재배치한다.
            - 띄어쓰기, 오타, 문장부호를 교정한다.
            - 화자 라벨이 명백히 잘못된 경우에만 speaker 를 교정한다.

            출력 형식:
            - 설명 없이 JSON 배열만 출력한다.
            - 형식: [{"id":"...","speaker":"...","text":"..."}, ...]
            - 입력과 동일한 개수, 동일한 id 를 유지한다.
            """;

    private RefinePrompt() {
    }

    /**
     * 입력 세그먼트를 id/speaker/text JSON 으로 직렬화한다.
     * start/end 타임스탬프는 모델 혼란 방지·토큰 절약을 위해 제외한다(병합 시 원본을 사용).
     */
    public static String buildUserPrompt(ObjectMapper objectMapper, List<TranscriptSegment> segments) {
        List<PromptSegment> payload = segments.stream()
                .map(segment -> new PromptSegment(segment.id(), segment.speaker(), segment.text()))
                .toList();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new DebateTrackerException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private record PromptSegment(String id, String speaker, String text) {
    }
}
