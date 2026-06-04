package com.debatetracker.debate.client;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import com.debatetracker.debate.service.transcript.UtteranceCorrector;
import com.debatetracker.infra.llm.client.LlmClient;
import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "llm.mode", havingValue = "real")
public class LlmUtteranceCorrectorAdapter implements UtteranceCorrector {

    private final LlmClient llmClient;

    @Override
    public List<RefinedSpeechSegment> refine(String debateId, String topic, List<RefinedSpeechSegment> context, List<SpeechSegment> batch) {
        log.debug("LLM 보정: debateId={}, topic={}, context={}건, batch={}건", debateId, topic, context.size(), batch.size());
        RefineRequest request = new RefineRequest(debateId, topic, toContexts(context), toTargets(batch));
        RefineResponse response = llmClient.refine(request);
        return toRefined(response.segments());
    }

    private List<TranscriptSegment> toContexts(List<RefinedSpeechSegment> context) {
        return context.stream()
                .map(segment -> new TranscriptSegment(
                        segment.getId(),
                        segment.getSpeaker(),
                        segment.getStartAt(),
                        segment.getEndAt(),
                        segment.getContent()))
                .toList();
    }

    private List<TranscriptSegment> toTargets(List<SpeechSegment> batch) {
        return batch.stream()
                .map(segment -> new TranscriptSegment(
                        segment.getId(),
                        segment.getSpeaker(),
                        segment.getStartAt(),
                        segment.getEndAt(),
                        segment.getContent()))
                .toList();
    }

    private List<RefinedSpeechSegment> toRefined(List<TranscriptSegment> segments) {
        return segments.stream()
                .map(segment -> new RefinedSpeechSegment(
                        segment.id(),
                        segment.text(),
                        segment.speaker(),
                        segment.start(),
                        segment.end()))
                .toList();
    }
}
