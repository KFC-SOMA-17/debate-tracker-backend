package com.debatetracker.debate.ws.message;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import java.util.List;

public record RefinedSegmentsResponse(List<RefinedSpeechSegment> segments) {
}
