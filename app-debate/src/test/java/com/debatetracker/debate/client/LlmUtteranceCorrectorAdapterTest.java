package com.debatetracker.debate.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import com.debatetracker.infra.llm.client.LlmClient;
import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LlmUtteranceCorrectorAdapterTest {

    private static final String DEBATE_ID = "1";
    private static final String TOPIC = "토론 주제";

    private LlmClient llmClient;
    private LlmUtteranceCorrectorAdapter adapter;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        adapter = new LlmUtteranceCorrectorAdapter(llmClient);
    }

    @Nested
    class Refine {

        @Test
        void 도메인_batch와_context를_RefineRequest로_조립해_LlmClient에_위임한다() {
            List<RefinedSpeechSegment> context = List.of(refined("ctx"));
            List<SpeechSegment> batch = List.of(speech("a"), speech("b"));
            when(llmClient.refine(any())).thenReturn(new RefineResponse(List.of(transcript("a"), transcript("b"))));

            adapter.refine(DEBATE_ID, TOPIC, context, batch);

            ArgumentCaptor<RefineRequest> captor = ArgumentCaptor.forClass(RefineRequest.class);
            verify(llmClient).refine(captor.capture());
            RefineRequest request = captor.getValue();
            TranscriptSegment target = request.targets().get(0);
            assertAll(
                    () -> assertThat(request.sessionId()).isEqualTo(DEBATE_ID),
                    () -> assertThat(request.topic()).isEqualTo(TOPIC),
                    () -> assertThat(request.contexts()).hasSize(1),
                    () -> assertThat(request.targets()).hasSize(2),
                    () -> assertThat(target.id()).isEqualTo("a"),
                    () -> assertThat(target.text()).isEqualTo("원본 a"),
                    () -> assertThat(target.speaker()).isEqualTo("Guest_0")
            );
        }

        @Test
        void RefineResponse의_TranscriptSegment를_RefinedSpeechSegment로_매핑해_반환한다() {
            List<SpeechSegment> batch = List.of(speech("a"));
            when(llmClient.refine(any())).thenReturn(new RefineResponse(List.of(transcript("a"))));

            List<RefinedSpeechSegment> result = adapter.refine(DEBATE_ID, TOPIC, List.of(), batch);

            RefinedSpeechSegment refined = result.get(0);
            assertAll(
                    () -> assertThat(result).hasSize(1),
                    () -> assertThat(refined.getId()).isEqualTo("a"),
                    () -> assertThat(refined.getContent()).isEqualTo("교정 a"),
                    () -> assertThat(refined.getSpeaker()).isEqualTo("Guest_0"),
                    () -> assertThat(refined.getStartAt()).isEqualByComparingTo(new BigDecimal("1.0")),
                    () -> assertThat(refined.getEndAt()).isEqualByComparingTo(new BigDecimal("2.0"))
            );
        }
    }

    private SpeechSegment speech(String id) {
        return new SpeechSegment(id, "원본 " + id, "Guest_0", new BigDecimal("1.0"), new BigDecimal("2.0"));
    }

    private RefinedSpeechSegment refined(String id) {
        return new RefinedSpeechSegment(id, "교정 " + id, "Guest_0", new BigDecimal("1.0"), new BigDecimal("2.0"));
    }

    private TranscriptSegment transcript(String id) {
        return new TranscriptSegment(id, "Guest_0", new BigDecimal("1.0"), new BigDecimal("2.0"), "교정 " + id);
    }
}
