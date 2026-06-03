package com.debatetracker.debate.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.ws.id.SimpleSegmentIdGenerator;
import com.debatetracker.debate.ws.message.MessageType;
import com.debatetracker.debate.domain.transcript.SpeechSegment;
import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.infra.stt.client.dto.SttSegment;
import com.debatetracker.infra.stt.client.event.TranscribeEvent;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;

class TranscribeEventListenerTest {

    private DebateSessionRepository sessionRepository;
    private WebSocketMessageSender messageSender;
    private TranscribeEventListener listener;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(DebateSessionRepository.class);
        messageSender = mock(WebSocketMessageSender.class);
        listener = new TranscribeEventListener(sessionRepository, messageSender, new SimpleSegmentIdGenerator());
    }

    @Test
    void 활성_세션이_있으면_전사_결과를_TRANSCRIPTION으로_매핑해_전송한다() {
        DebateSession session = new DebateSession("1", mock(WebSocketSession.class));
        when(sessionRepository.findByDebateId("1")).thenReturn(Optional.of(session));
        SttSegment segment = new SttSegment(
                new BigDecimal("1.200"), new BigDecimal("4.800"), "Guest_1", "안녕하세요");

        listener.onTranscribe(new TranscribeEvent("1", segment));

        ArgumentCaptor<WebSocketMessage> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(messageSender).send(any(DebateSession.class), captor.capture());
        WebSocketMessage sent = captor.getValue();
        SpeechSegment data = (SpeechSegment) sent.data();
        assertAll(
                () -> assertThat(sent.type()).isEqualTo(MessageType.TRANSCRIPTION),
                () -> assertThat(sent.debateId()).isEqualTo(1L),
                () -> assertThat(data.getId()).isNotNull(),
                () -> assertThat(data.getContent()).isEqualTo("안녕하세요"),
                () -> assertThat(data.getSpeaker()).isEqualTo("Guest_1"),
                () -> assertThat(data.getStartAt()).isEqualByComparingTo("1.200"),
                () -> assertThat(data.getEndAt()).isEqualByComparingTo("4.800")
        );
    }

    @Test
    void 활성_세션이_없으면_아무것도_전송하지_않는다() {
        when(sessionRepository.findByDebateId("1")).thenReturn(Optional.empty());

        listener.onTranscribe(new TranscribeEvent("1", new SttSegment(
                BigDecimal.ZERO, BigDecimal.ONE, "Guest_1", "텍스트")));

        verify(messageSender, never()).send(any(DebateSession.class), any());
    }
}
