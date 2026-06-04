package com.debatetracker.debate.service.transcript;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import java.util.List;

/**
 * STT raw 세그먼트를 이전 문맥과 함께 LLM 으로 보정하는 도메인 wrapper 포트.
 *
 * <p>infra-llm 의 벤더 중립 LlmClient 를 감싸는 자리다. 실제 LlmClient 가 이 브랜치에 합류하기 전까지는
 * MockUtteranceCorrector 가 기본 구현을 제공한다. 실구현은 LlmClient.refine() 의 비동기 응답을
 * 이 wrapper 안에서 동기로 변환(join)해 반환한다.
 *
 * <p>불변식: 반환 리스트는 batch 와 <b>같은 개수·같은 id</b> 를 가져야 한다.
 */
public interface UtteranceCorrector {

    List<RefinedSpeechSegment> refine(String topic, List<RefinedSpeechSegment> context, List<SpeechSegment> batch);
}
