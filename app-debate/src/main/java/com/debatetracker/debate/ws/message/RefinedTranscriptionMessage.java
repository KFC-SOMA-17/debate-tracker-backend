package com.debatetracker.debate.ws.message;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import java.util.List;

/**
 * LLM 보정이 완료된 세그먼트 묶음을 클라이언트로 전송하는 메시지. 클라이언트는 같은 id 의 raw
 * TRANSCRIPTION 을 이 교정본으로 교체한다.
 */
public class RefinedTranscriptionMessage extends WebSocketMessage {

    private final RefinedSegmentsResponse segments;

    public RefinedTranscriptionMessage(long debateId, List<RefinedSpeechSegment> segments) {
        super(debateId, MessageType.REFINED_TRANSCRIPTION);
        this.segments = new RefinedSegmentsResponse(segments);
    }

    @Override
    public RefinedSegmentsResponse data() {
        return segments;
    }
}
