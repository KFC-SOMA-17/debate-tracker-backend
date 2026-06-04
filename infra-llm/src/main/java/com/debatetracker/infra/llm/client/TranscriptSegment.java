package com.debatetracker.infra.llm.client;

import java.math.BigDecimal;

/**
 * 정제 대상/결과 발화 세그먼트. id/start/end 는 골격(불변), text(+speaker)만 정제 대상.
 * infra-llm 이 소유하는 기능 계약 — 도메인 모듈과의 중복은 멀티모듈 전략 §1 에 따라 수용한다.
 */
public record TranscriptSegment(
        String id,
        String speaker,
        BigDecimal start,
        BigDecimal end,
        String text
) {
}
