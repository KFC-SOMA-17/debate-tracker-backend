package com.debatetracker.debate.service.transcript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.domain.debate.Debate;
import com.debatetracker.debate.domain.debate.DebateRepository;
import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.transcript.RefinedSpeechSegment;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import com.debatetracker.debate.domain.transcript.repository.TranscriptBufferRepository;
import com.debatetracker.debate.ws.message.MessageType;
import com.debatetracker.debate.ws.message.RefinedSegmentsResponse;
import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;

class TranscribeRefiningServiceTest {

    private static final String DEBATE_ID = "1";
    private static final long DEBATE_ID_VALUE = 1L;
    private static final String TOPIC = "토론 주제";

    private TranscriptBufferRepository bufferRepository;
    private UtteranceCorrector corrector;
    private WebSocketMessageSender messageSender;
    private DebateRepository debateRepository;
    private SpeechBoxService speechBoxService;
    private TranscribeRefiningService service;
    private DebateSession session;

    @BeforeEach
    void setUp() {
        bufferRepository = mock(TranscriptBufferRepository.class);
        corrector = mock(UtteranceCorrector.class);
        messageSender = mock(WebSocketMessageSender.class);
        debateRepository = mock(DebateRepository.class);
        speechBoxService = mock(SpeechBoxService.class);
        service = new TranscribeRefiningService(
                bufferRepository, corrector, messageSender, debateRepository, speechBoxService);
        session = new DebateSession(DEBATE_ID, mock(WebSocketSession.class));
        when(debateRepository.findById(DEBATE_ID_VALUE)).thenReturn(new Debate(DEBATE_ID_VALUE, TOPIC));
    }

    @Nested
    class RefineSession {

        @Test
        void 보정에_성공하면_raw를_제거하고_refined에_축적하며_REFINED_TRANSCRIPTION을_전송한다() {
            List<SpeechSegment> batch = List.of(speech("a"), speech("b"), speech("c"));
            when(bufferRepository.rawSize(DEBATE_ID)).thenReturn(3L);
            when(bufferRepository.peekRaw(DEBATE_ID, 3)).thenReturn(batch);
            when(bufferRepository.recentRefined(DEBATE_ID, 5)).thenReturn(List.of());
            List<RefinedSpeechSegment> corrected = List.of(refined("a"), refined("b"), refined("c"));
            when(corrector.refine(any(), eq(List.of()), eq(batch))).thenReturn(corrected);

            service.refineSession(session);

            ArgumentCaptor<WebSocketMessage> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messageSender).send(any(DebateSession.class), captor.capture());
            WebSocketMessage sent = captor.getValue();
            RefinedSegmentsResponse data = (RefinedSegmentsResponse) sent.data();
            assertAll(
                    () -> verify(bufferRepository).trimRaw(DEBATE_ID, 3),
                    () -> verify(bufferRepository).appendRefined(DEBATE_ID, corrected),
                    () -> verify(speechBoxService).persist(DEBATE_ID_VALUE, corrected),
                    () -> assertThat(sent.type()).isEqualTo(MessageType.REFINED_TRANSCRIPTION),
                    () -> assertThat(sent.debateId()).isEqualTo(DEBATE_ID_VALUE),
                    () -> assertThat(data.segments()).hasSize(3)
            );
        }

        @Test
        void raw가_없으면_보정을_호출하지_않고_아무것도_전송하지_않는다() {
            when(bufferRepository.rawSize(DEBATE_ID)).thenReturn(0L);

            service.refineSession(session);

            assertAll(
                    () -> verify(corrector, never()).refine(anyString(), anyList(), anyList()),
                    () -> verify(bufferRepository, never()).trimRaw(anyString(), anyInt()),
                    () -> verify(messageSender, never()).send(any(DebateSession.class), any())
            );
        }

        @Test
        void raw가_MAX_BATCH보다_많아도_앞쪽_5개와_최근_refined_5개만_요청한다() {
            List<SpeechSegment> batch = List.of(speech("a"), speech("b"), speech("c"), speech("d"), speech("e"));
            when(bufferRepository.rawSize(DEBATE_ID)).thenReturn(8L);
            when(bufferRepository.peekRaw(DEBATE_ID, 5)).thenReturn(batch);
            when(bufferRepository.recentRefined(DEBATE_ID, 5)).thenReturn(List.of());
            when(corrector.refine(any(), anyList(), eq(batch)))
                    .thenReturn(List.of(refined("a"), refined("b"), refined("c"), refined("d"), refined("e")));

            service.refineSession(session);

            assertAll(
                    () -> verify(bufferRepository).peekRaw(DEBATE_ID, 5),
                    () -> verify(bufferRepository).recentRefined(DEBATE_ID, 5),
                    () -> verify(bufferRepository).trimRaw(DEBATE_ID, 5)
            );
        }

        @Test
        void 보정_결과가_요청과_불일치하면_예외를_삼키고_버퍼를_변경하지_않는다() {
            List<SpeechSegment> batch = List.of(speech("a"), speech("b"));
            when(bufferRepository.rawSize(DEBATE_ID)).thenReturn(2L);
            when(bufferRepository.peekRaw(DEBATE_ID, 2)).thenReturn(batch);
            when(bufferRepository.recentRefined(DEBATE_ID, 5)).thenReturn(List.of());
            when(corrector.refine(any(), anyList(), anyList())).thenReturn(List.of(refined("a"))); // 개수 불일치

            assertAll(
                    () -> assertThatCode(() -> service.refineSession(session)).doesNotThrowAnyException(),
                    () -> verify(bufferRepository, never()).trimRaw(anyString(), anyInt()),
                    () -> verify(bufferRepository, never()).appendRefined(anyString(), anyList()),
                    () -> verify(messageSender, never()).send(any(DebateSession.class), any())
            );
        }

        @Test
        void 보정_호출이_실패하면_예외를_삼키고_raw를_유지한다() {
            when(bufferRepository.rawSize(DEBATE_ID)).thenReturn(1L);
            when(bufferRepository.peekRaw(DEBATE_ID, 1)).thenReturn(List.of(speech("a")));
            when(bufferRepository.recentRefined(DEBATE_ID, 5)).thenReturn(List.of());
            when(corrector.refine(any(), anyList(), anyList())).thenThrow(new RuntimeException("보정 오류"));

            assertAll(
                    () -> assertThatCode(() -> service.refineSession(session)).doesNotThrowAnyException(),
                    () -> verify(bufferRepository, never()).trimRaw(anyString(), anyInt()),
                    () -> verify(bufferRepository, never()).appendRefined(anyString(), anyList()),
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
