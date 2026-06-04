package com.debatetracker.debate.infrastructure.persistence;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import com.debatetracker.debate.domain.transcript.repository.TranscriptBufferRepository;
import com.debatetracker.debate.infrastructure.persistence.redis.TranscriptBufferRedisRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TranscriptBufferDomainRepository implements TranscriptBufferRepository {

    private final TranscriptBufferRedisRepository bufferStore;

    @Override
    public void appendRaw(String debateId, SpeechSegment segment) {
        bufferStore.appendRaw(debateId, segment);
    }

    @Override
    public long rawSize(String debateId) {
        return bufferStore.countRawSegments(debateId);
    }

    @Override
    public List<SpeechSegment> peekRaw(String debateId, int count) {
        return bufferStore.peekRaw(debateId, count);
    }

    @Override
    public void trimRaw(String debateId, int count) {
        bufferStore.trimRaw(debateId, count);
    }

    @Override
    public List<RefinedSpeechSegment> recentRefined(String debateId, int n) {
        return bufferStore.recentRefined(debateId, n);
    }

    @Override
    public void appendRefined(String debateId, List<RefinedSpeechSegment> segments) {
        bufferStore.appendRefined(debateId, segments);
    }

    @Override
    public void clear(String debateId) {
        bufferStore.clear(debateId);
    }
}
