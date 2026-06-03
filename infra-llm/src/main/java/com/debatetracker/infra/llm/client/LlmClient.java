package com.debatetracker.infra.llm.client;

/**
 * 벤더 중립 LLM 클라이언트. 범용 호출이 아니라 기능별 메서드를 노출한다.
 * 시그니처에 Spring AI / 벤더 SDK 타입을 노출하지 않는다.
 *
 * 비동기 반환 — 정제 latency 가 WebSocket broadcast 스레드를 블로킹하지 않도록 한다
 */
public interface LlmClient {

    /**
     * STT 세그먼트를 문맥 기반으로 정제한다. id/start/end 골격은 보존되고 text(+speaker)만 정제된다.
     */
    RefineResponse refine(RefineRequest request);
}
