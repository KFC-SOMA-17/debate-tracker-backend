package com.debatetracker.debate.domain.transcript.repository;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import java.util.List;

/**
 * 토론별 전사 버퍼 포트. raw(원본) 세그먼트와 refined(교정) 세그먼트를 각각 리스트로 누적한다.
 * raw 는 LLM 교정 트리거가 앞에서부터 소비(peek → trim)하고, refined 는 교정 결과를 뒤에 축적한다.
 */
public interface TranscriptBufferRepository {

    void appendRaw(String debateId, SpeechSegment segment);

    long rawSize(String debateId);

    /**
     * raw 앞쪽 count 개를 삭제하지 않고 조회한다 (LRANGE 0 ~ count-1).
     */
    List<SpeechSegment> peekRaw(String debateId, int count);

    /**
     * 방금 처리한 raw 앞쪽 count 개를 제거한다 (LTRIM count ~ -1).
     */
    void trimRaw(String debateId, int count);

    /**
     * 최근 refined n 개를 조회한다 (LRANGE -n ~ -1). LLM 요청의 이전 문맥으로 쓰인다.
     */
    List<RefinedSpeechSegment> recentRefined(String debateId, int n);

    void appendRefined(String debateId, List<RefinedSpeechSegment> segments);

    void clear(String debateId);
}
