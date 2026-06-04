package com.debatetracker.debate.service.transcript;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import java.util.List;

public interface UtteranceCorrector {

    List<RefinedSpeechSegment> refine(String debateId, String topic, List<RefinedSpeechSegment> context, List<SpeechSegment> batch);
}
