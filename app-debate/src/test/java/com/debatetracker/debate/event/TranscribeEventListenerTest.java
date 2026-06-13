package com.debatetracker.debate.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.ws.id.SimpleSegmentIdGenerator;
import com.debatetracker.debate.ws.message.MessageType;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import com.debatetracker.debate.domain.transcript.repository.TranscriptBufferRepository;
import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.infra.stt.client.dto.SttSegment;
import com.debatetracker.infra.stt.client.event.TranscribeEvent;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;

class TranscribeEventListenerTest {

    private DebateSessionRepository sessionRepository;
    private WebSocketMessageSender messageSender;
    private TranscriptBufferRepository bufferRepository;
    private TranscribeEventListener listener;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(DebateSessionRepository.class);
        messageSender = mock(WebSocketMessageSender.class);
        bufferRepository = mock(TranscriptBufferRepository.class);
        listener = new TranscribeEventListener(
                sessionRepository, messageSender, new SimpleSegmentIdGenerator(), bufferRepository);
    }

    @Nested
    class OnTranscribe {

        @Test
        void 활성_세션이_있으면_전사_결과를_TRANSCRIPTION으로_매핑해_전송한다() {
            String debateId = "1";
            long debateIdValue = 1L;
            String speaker = "Guest_1";
            String content = "안녕하세요";
            BigDecimal startAt = new BigDecimal("1.200");
            BigDecimal endAt = new BigDecimal("4.800");
            DebateSession session = new DebateSession(debateId, mock(WebSocketSession.class));
            when(sessionRepository.findByDebateId(debateId)).thenReturn(Optional.of(session));
            SttSegment segment = new SttSegment(startAt, endAt, speaker, content);

            listener.onTranscribe(new TranscribeEvent(debateId, segment));

            ArgumentCaptor<WebSocketMessage> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messageSender).broadcast(eq(debateId), captor.capture());
            WebSocketMessage sent = captor.getValue();
            SpeechSegment data = (SpeechSegment) sent.data();
            assertAll(
                    () -> verify(bufferRepository).appendRaw(any(), any(SpeechSegment.class)),
                    () -> assertThat(sent.type()).isEqualTo(MessageType.TRANSCRIPTION),
                    () -> assertThat(sent.debateId()).isEqualTo(debateIdValue),
                    () -> assertThat(data.getId()).isNotNull(),
                    () -> assertThat(data.getContent()).isEqualTo(content),
                    () -> assertThat(data.getSpeaker()).isEqualTo(speaker),
                    () -> assertThat(data.getStartAt()).isEqualByComparingTo(startAt),
                    () -> assertThat(data.getEndAt()).isEqualByComparingTo(endAt)
            );
        }

        @Test
        void 활성_세션이_없으면_아무것도_전송하지_않는다() {
            String debateId = "1";
            when(sessionRepository.findByDebateId(debateId)).thenReturn(Optional.empty());

            listener.onTranscribe(new TranscribeEvent(debateId, new SttSegment(
                    BigDecimal.ZERO, BigDecimal.ONE, "Guest_1", "텍스트")));

            assertAll(
                    () -> verify(messageSender, never()).broadcast(any(), any()),
                    () -> verify(bufferRepository, never()).appendRaw(any(), any())
            );
        }
    }
}
