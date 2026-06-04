package com.debatetracker.debate.ws.message;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import java.util.List;

/**
 * REFINED_TRANSCRIPTION 메시지의 data 페이로드. 한 번의 교정으로 갱신된 세그먼트 묶음을 담는다.
 */
public record RefinedSegmentsResponse(List<RefinedSpeechSegment> segments) {
}
