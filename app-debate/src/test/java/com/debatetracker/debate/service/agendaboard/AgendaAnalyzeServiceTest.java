package com.debatetracker.debate.service.agendaboard;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.domain.agendaboard.AgendaBoard;
import com.debatetracker.debate.domain.agendaboard.repository.AgendaBoardRepository;
import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.transcript.SpeechBox;
import com.debatetracker.debate.domain.transcript.repository.SpeechBoxRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AgendaAnalyzeServiceTest {

    private static final String DEBATE_ID = "1";
    private static final long DEBATE_ID_VALUE = 1L;

    private SpeechBoxRepository speechBoxRepository;
    private AgendaBoardRepository agendaBoardRepository;
    private DebateAgendaAnalyzer agendaAnalyzer;
    private AgendaAnalyzeService service;
    private DebateSession session;

    @BeforeEach
    void setUp() {
        speechBoxRepository = mock(SpeechBoxRepository.class);
        agendaBoardRepository = mock(AgendaBoardRepository.class);
        agendaAnalyzer = mock(DebateAgendaAnalyzer.class);
        service = new AgendaAnalyzeService(speechBoxRepository, agendaBoardRepository, agendaAnalyzer);
        session = new DebateSession(DEBATE_ID);
    }

    @Nested
    class AnalyzeSession {

        @Test
        void 발화가_있으면_조회한_발화와_보드로_분석하고_결과를_upsert한다() {
            List<SpeechBox> speeches = List.of(speech());
            AgendaBoard before = new AgendaBoard(DEBATE_ID_VALUE, List.of());
            AgendaBoard analyzed = new AgendaBoard(DEBATE_ID_VALUE, List.of());
            when(speechBoxRepository.findAllByDebateId(DEBATE_ID_VALUE)).thenReturn(speeches);
            when(agendaBoardRepository.findByDebateId(DEBATE_ID_VALUE)).thenReturn(before);
            when(agendaAnalyzer.analyze(DEBATE_ID_VALUE, speeches, before)).thenReturn(analyzed);

            service.analyzeSession(session);

            assertAll(
                    () -> verify(agendaAnalyzer).analyze(DEBATE_ID_VALUE, speeches, before),
                    () -> verify(agendaBoardRepository).upsert(analyzed)
            );
        }

        @Test
        void 발화가_없으면_분석도_저장도_하지_않는다() {
            when(speechBoxRepository.findAllByDebateId(DEBATE_ID_VALUE)).thenReturn(List.of());

            service.analyzeSession(session);

            assertAll(
                    () -> verify(agendaAnalyzer, never()).analyze(anyLong(), anyList(), any()),
                    () -> verify(agendaBoardRepository, never()).upsert(any())
            );
        }

        @Test
        void 분석이_실패하면_예외를_삼키고_저장하지_않는다() {
            when(speechBoxRepository.findAllByDebateId(DEBATE_ID_VALUE)).thenReturn(List.of(speech()));
            when(agendaBoardRepository.findByDebateId(DEBATE_ID_VALUE))
                    .thenReturn(new AgendaBoard(DEBATE_ID_VALUE, List.of()));
            when(agendaAnalyzer.analyze(anyLong(), anyList(), any())).thenThrow(new RuntimeException("분석 오류"));

            assertAll(
                    () -> assertThatCode(() -> service.analyzeSession(session)).doesNotThrowAnyException(),
                    () -> verify(agendaBoardRepository, never()).upsert(any())
            );
        }
    }

    private SpeechBox speech() {
        return new SpeechBox(1L, DEBATE_ID_VALUE, "Guest_0", "발화", new BigDecimal("1.0"), new BigDecimal("2.0"));
    }
}
