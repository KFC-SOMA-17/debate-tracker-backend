package com.debatetracker.debate.service.transcript;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * UtteranceCorrector 의 mock 구현. 실제 LLM 호출 없이 batch 의 id/start/end 골격을 보존한 채
 * content 만 가볍게 정규화(앞뒤 공백 제거)해 RefinedSpeechSegment 로 변환한다.
 *
 * <p>TODO: infra-llm 의 LlmClient.refine() 위임 구현으로 교체한다. (현재 LlmClient 는 feat/#27 에만 존재)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "llm.mode", havingValue = "mock", matchIfMissing = true)
public class MockUtteranceCorrector implements UtteranceCorrector {

    @Override
    public List<RefinedSpeechSegment> refine(String topic, List<RefinedSpeechSegment> context, List<SpeechSegment> batch) {
        log.debug("mock 보정: topic={}, context={}건, batch={}건", topic, context.size(), batch.size());
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
