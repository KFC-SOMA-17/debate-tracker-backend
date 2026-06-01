package com.debatetracker.infra.llm.client;

import java.util.List;

/**
 * 세그먼트 보정 기능의 입력 계약.
 *
 * @param sessionId 메트릭 라벨링용(세션별 슬라이스). 정제 로직에는 관여하지 않는다.
 * @param segments  정제 대상 세그먼트. 전 세그먼트를 동시에 넘겨 단어 경계 문맥을 확보한다.
 */
public record RefineRequest(
        String sessionId,
        List<TranscriptSegment> segments
) {

    public RefineRequest {
        segments = List.copyOf(segments);
    }
}
