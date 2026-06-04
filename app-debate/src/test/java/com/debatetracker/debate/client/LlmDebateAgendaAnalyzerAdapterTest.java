package com.debatetracker.debate.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.domain.agendaboard.Agenda;
import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.domain.agendaboard.Claim;
import com.debatetracker.debate.domain.agendaboard.Evidence;
import com.debatetracker.debate.domain.agendaboard.EvidenceType;
import com.debatetracker.debate.domain.agendaboard.Stance;
import com.debatetracker.debate.domain.transcript.SpeechBox;
import com.debatetracker.infra.llm.client.ExtractAgenda;
import com.debatetracker.infra.llm.client.ExtractAgendaRequest;
import com.debatetracker.infra.llm.client.ExtractAgendaResponse;
import com.debatetracker.infra.llm.client.ExtractClaim;
import com.debatetracker.infra.llm.client.ExtractEvidenceType;
import com.debatetracker.infra.llm.client.ExtractStance;
import com.debatetracker.infra.llm.client.ExtractedEvidence;
import com.debatetracker.infra.llm.client.LlmClient;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LlmDebateAgendaAnalyzerAdapterTest {

    private static final long DEBATE_ID = 1L;

    private LlmClient llmClient;
    private LlmDebateAgendaAnalyzerAdapter adapter;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        adapter = new LlmDebateAgendaAnalyzerAdapter(llmClient);
    }

    @Nested
    class Analyze {

        @Test
        void 발화와_이전_보드를_LLM_요청으로_매핑한다() {
            List<SpeechBox> speeches = List.of(
                    new SpeechBox(10L, DEBATE_ID, "Guest_0", "안녕하세요", new BigDecimal("1.0"), new BigDecimal("2.0")));
            AgendaBoard before = new AgendaBoard(DEBATE_ID, List.of(
                    new Agenda(5L, DEBATE_ID, "쟁점", null, null, List.of(
                            new Claim(7L, 5L, "주장", Stance.PROS, null, null, List.of(
                                    new Evidence(9L, 7L, "근거", EvidenceType.STATISTICS, null, null)))))));
            when(llmClient.extract(any())).thenReturn(new ExtractAgendaResponse(List.of()));

            adapter.analyze(DEBATE_ID, speeches, before);

            ArgumentCaptor<ExtractAgendaRequest> captor = ArgumentCaptor.forClass(ExtractAgendaRequest.class);
            verify(llmClient).extract(captor.capture());
            ExtractAgendaRequest request = captor.getValue();
            TranscriptSegment segment = request.contexts().get(0);
            ExtractAgenda agenda = request.beforeAgendas().get(0);
            ExtractClaim claim = agenda.claims().get(0);
            ExtractedEvidence evidence = claim.evidences().get(0);
            assertAll(
                    () -> assertThat(request.sessionId()).isEqualTo("1"),
                    () -> assertThat(segment.id()).isEqualTo("10"),
                    () -> assertThat(segment.text()).isEqualTo("안녕하세요"),
                    () -> assertThat(agenda.id()).isEqualTo("5"),
                    () -> assertThat(claim.id()).isEqualTo("7"),
                    () -> assertThat(claim.stance()).isEqualTo(ExtractStance.PROS),
                    () -> assertThat(evidence.id()).isEqualTo("9"),
                    () -> assertThat(evidence.type()).isEqualTo(ExtractEvidenceType.STATISTICS)
            );
        }

        @Test
        void LLM_응답을_도메인_보드로_매핑하고_신규는_식별자가_null이다() {
            ExtractAgendaResponse response = new ExtractAgendaResponse(List.of(
                    new ExtractAgenda("5", "쟁점", List.of(
                            new ExtractClaim(null, ExtractStance.CONS, "신규 주장", List.of(
                                    new ExtractedEvidence(null, ExtractEvidenceType.EXAMPLE, "신규 근거")))))));
            when(llmClient.extract(any())).thenReturn(response);

            AgendaBoard board = adapter.analyze(DEBATE_ID, List.of(), new AgendaBoard(DEBATE_ID, List.of()));

            Agenda agenda = board.getAgendas().get(0);
            Claim claim = agenda.getClaims().get(0);
            Evidence evidence = claim.getEvidences().get(0);
            assertAll(
                    () -> assertThat(board.getDebateId()).isEqualTo(DEBATE_ID),
                    () -> assertThat(agenda.getId()).isEqualTo(5L),
                    () -> assertThat(agenda.getContent()).isEqualTo("쟁점"),
                    () -> assertThat(claim.getId()).isNull(),
                    () -> assertThat(claim.getStance()).isEqualTo(Stance.CONS),
                    () -> assertThat(evidence.getId()).isNull(),
                    () -> assertThat(evidence.getType()).isEqualTo(EvidenceType.EXAMPLE)
            );
        }
    }
}
