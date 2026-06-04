package com.debatetracker.debate.domain.transcript.repository;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import java.util.List;

public interface TranscriptBufferRepository {

    void appendRaw(String debateId, SpeechSegment segment);

    long rawSize(String debateId);

    List<SpeechSegment> peekRaw(String debateId, int count);

    void trimRaw(String debateId, int count);

    List<RefinedSpeechSegment> recentRefined(String debateId, int n);

    void appendRefined(String debateId, List<RefinedSpeechSegment> segments);

    void clear(String debateId);
}
