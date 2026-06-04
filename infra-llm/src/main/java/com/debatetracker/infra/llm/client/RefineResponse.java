package com.debatetracker.infra.llm.client;

import java.util.List;

/**
 * 세그먼트 보정 기능의 출력 계약. 입력과 id 집합·개수가 동일하며 id/start/end 골격은 보존된다.
 */
public record RefineResponse(
        List<TranscriptSegment> segments
) {

    public RefineResponse {
        segments = List.copyOf(segments);
    }
}
