package com.debatetracker.debate.ws.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.service.debate.DebateStreamingService;
import com.debatetracker.debate.ws.message.MessageType;
import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.debate.domain.transcript.repository.TranscriptBufferRepository;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.infra.stt.client.SttClient;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class SttWebSocketHandlerTest {

    private SttClient sttClient;
    private DebateSessionRepository sessionRepository;
    private WebSocketMessageSender messageSender;
    private SttWebSocketHandler handler;

    private WebSocketSession connection;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        sttClient = mock(SttClient.class);
        sessionRepository = mock(DebateSessionRepository.class);
        messageSender = mock(WebSocketMessageSender.class);
        DebateStreamingService debateStreamingService = new DebateStreamingService(
                sttClient, sessionRepository, mock(TranscriptBufferRepository.class));
        handler = new SttWebSocketHandler(debateStreamingService, messageSender);

        connection = mock(WebSocketSession.class);
        attributes = new HashMap<>();
        when(connection.getAttributes()).thenReturn(attributes);
    }

    @Nested
    class HandleControlMessage {

        @Test
        void START_제어메시지를_받으면_전사를_시작하고_세션을_저장하며_DEBATE_START를_전송한다() throws Exception {
            String debateId = "1";
            long debateIdValue = 1L;

            handler.handleTextMessage(connection, new TextMessage(
                    "{\"type\":\"START\",\"sessionId\":\"" + debateId + "\"}"));

            WebSocketMessage sent = captureSentMessage();
            assertAll(
                    () -> verify(sttClient).startStreaming(debateId),
                    () -> verify(sessionRepository).save(any(DebateSession.class)),
                    () -> assertThat(attributes).containsEntry("debateId", debateId),
                    () -> assertThat(sent.type()).isEqualTo(MessageType.DEBATE_START),
                    () -> assertThat(sent.debateId()).isEqualTo(debateIdValue)
            );
        }

        @Test
        void STOP_제어메시지를_받으면_전사를_종료하고_DEBATE_END를_전송하며_세션을_삭제한다() throws Exception {
            String debateId = "1";
            long debateIdValue = 1L;
            attributes.put("debateId", debateId);
            DebateSession session = new DebateSession(debateId, connection);
            when(sessionRepository.existsByDebateId(debateId)).thenReturn(true);
            when(sessionRepository.deleteByDebateId(debateId)).thenReturn(Optional.of(session));

            handler.handleTextMessage(connection, new TextMessage(
                    "{\"type\":\"STOP\",\"sessionId\":\"" + debateId + "\"}"));

            WebSocketMessage sent = captureSentMessage();
            assertAll(
                    () -> verify(sttClient).stopStreaming(debateId),
                    () -> verify(sessionRepository).deleteByDebateId(debateId),
                    () -> assertThat(sent.type()).isEqualTo(MessageType.DEBATE_END),
                    () -> assertThat(sent.debateId()).isEqualTo(debateIdValue)
            );
        }
    }

    @Nested
    class HandleBinaryMessage {

        @Test
        void 바이너리_오디오를_받으면_해당_debateId로_오디오_청크를_전송한다() {
            String debateId = "1";
            attributes.put("debateId", debateId);
            byte[] pcm = {1, 2, 3, 4};

            handler.handleBinaryMessage(connection, new BinaryMessage(pcm));

            verify(sttClient).sendAudioChunk(eq(debateId), eq(pcm));
        }

        @Test
        void START_이전_바이너리는_무시한다() {
            handler.handleBinaryMessage(connection, new BinaryMessage(new byte[]{1}));

            verify(sttClient, never()).sendAudioChunk(any(), any());
        }
    }

    @Nested
    class AfterConnectionClosed {

        @Test
        void 연결이_종료되면_남아있는_세션을_정리한다() {
            String debateId = "1";
            attributes.put("debateId", debateId);
            when(sessionRepository.existsByDebateId(debateId)).thenReturn(true);
            when(sessionRepository.deleteByDebateId(debateId))
                    .thenReturn(Optional.of(new DebateSession(debateId, connection)));

            handler.afterConnectionClosed(connection, CloseStatus.NORMAL);

            verify(sttClient).stopStreaming(debateId);
        }
    }

    private WebSocketMessage captureSentMessage() {
        ArgumentCaptor<WebSocketMessage> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(messageSender).send(any(DebateSession.class), captor.capture());
        return captor.getValue();
    }
}
