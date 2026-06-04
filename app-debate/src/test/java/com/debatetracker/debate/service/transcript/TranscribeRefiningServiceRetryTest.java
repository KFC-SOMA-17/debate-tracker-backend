package com.debatetracker.debate.service.transcript;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.domain.debate.DebateRepository;
import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import com.debatetracker.debate.domain.transcript.repository.TranscriptBufferRepository;
import com.debatetracker.debate.service.BaseServiceTest;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketSession;

/**
 * {@code @Retryable} 은 AOP 프록시로 동작하므로 Spring 컨텍스트에서만 검증된다.
 */
class TranscribeRefiningServiceRetryTest extends BaseServiceTest {

    private static final String DEBATE_ID = "1";
    private static final long DEBATE_ID_VALUE = 1L;
    private static final String TOPIC = "토론 주제";

    @Autowired
    private TranscribeRefiningService refiningService;

    @MockitoBean
    private TranscriptBufferRepository bufferRepository;

    @MockitoBean
    private UtteranceCorrector corrector;

    @MockitoBean
    private WebSocketMessageSender messageSender;

    @MockitoBean
    private DebateRepository debateRepository;

    @Nested
    class RefineSession {

        @Test
        void 보정이_일시적으로_실패하면_재시도해_성공하면_정상_처리된다() {
            DebateSession session = new DebateSession(DEBATE_ID, mock(WebSocketSession.class));
            List<SpeechSegment> batch = List.of(speech("a"));
            when(bufferRepository.rawSize(DEBATE_ID)).thenReturn(1L);
            when(bufferRepository.peekRaw(DEBATE_ID, 1)).thenReturn(batch);
            when(bufferRepository.recentRefined(DEBATE_ID, 5)).thenReturn(List.of());
            when(debateRepository.findById(DEBATE_ID_VALUE)).thenReturn(new Debate(DEBATE_ID_VALUE, TOPIC));
            when(corrector.refine(any(), anyList(), anyList()))
                    .thenThrow(new RuntimeException("일시 오류"))
                    .thenThrow(new RuntimeException("일시 오류"))
                    .thenReturn(List.of(refined("a")));

            refiningService.refineSession(session);

            assertAll(
                    () -> verify(corrector, times(3)).refine(any(), anyList(), anyList()),
                    () -> verify(bufferRepository).trimRaw(DEBATE_ID, 1),
                    () -> verify(bufferRepository).appendRefined(any(), anyList()),
                    () -> verify(messageSender).send(any(DebateSession.class), any())
            );
        }

        @Test
        void 재시도를_모두_소진하면_recover가_처리하고_raw를_유지한다() {
            DebateSession session = new DebateSession(DEBATE_ID, mock(WebSocketSession.class));
            when(bufferRepository.rawSize(DEBATE_ID)).thenReturn(1L);
            when(bufferRepository.peekRaw(DEBATE_ID, 1)).thenReturn(List.of(speech("a")));
            when(bufferRepository.recentRefined(DEBATE_ID, 5)).thenReturn(List.of());
            when(debateRepository.findById(DEBATE_ID_VALUE)).thenReturn(new Debate(DEBATE_ID_VALUE, TOPIC));
            when(corrector.refine(any(), anyList(), anyList())).thenThrow(new RuntimeException("영구 오류"));

            refiningService.refineSession(session);

            assertAll(
                    () -> verify(corrector, times(3)).refine(any(), anyList(), anyList()),
                    () -> verify(bufferRepository, never()).trimRaw(anyString(), anyInt()),
                    () -> verify(messageSender, never()).send(any(DebateSession.class), any())
            );
        }
    }

    private SpeechSegment speech(String id) {
        return new SpeechSegment(id, "원본 " + id, "Guest_0", new BigDecimal("1.0"), new BigDecimal("2.0"));
    }

    private RefinedSpeechSegment refined(String id) {
        return new RefinedSpeechSegment(id, "교정 " + id, "Guest_0", new BigDecimal("1.0"), new BigDecimal("2.0"));
    }
}
