package com.debatetracker.debate.service.transcript;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "llm.mode", havingValue = "mock", matchIfMissing = true)
public class MockUtteranceCorrector implements UtteranceCorrector {

    @Override
    public List<RefinedSpeechSegment> refine(String debateId, String topic, List<RefinedSpeechSegment> context, List<SpeechSegment> batch) {
        log.debug("mock 보정: debateId={}, topic={}, context={}건, batch={}건", debateId, topic, context.size(), batch.size());
        List<RefinedSpeechSegment> refined = new ArrayList<>(batch.size());
        for (SpeechSegment segment : batch) {
            refined.add(new RefinedSpeechSegment(
                    segment.getId(),
                    normalize(segment.getContent()),
                    segment.getSpeaker(),
                    segment.getStartAt(),
                    segment.getEndAt()
            ));
        }
        return refined;
    }

    private String normalize(String content) {
        return content == null ? null : content.strip();
    }
}
