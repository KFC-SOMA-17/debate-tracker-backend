package com.debatetracker.debate.ws.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.ws.message.MessageType;
import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.domain.session.DebateSessionRepository;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.infra.stt.client.SttClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
        handler = new SttWebSocketHandler(sttClient, sessionRepository, messageSender, new ObjectMapper());

        connection = mock(WebSocketSession.class);
        attributes = new HashMap<>();
        when(connection.getAttributes()).thenReturn(attributes);
    }

    @Test
    void START_제어메시지를_받으면_전사를_시작하고_세션을_저장하며_DEBATE_START를_전송한다() throws Exception {
        handler.handleTextMessage(connection, new TextMessage("{\"type\":\"START\",\"sessionId\":\"1\"}"));

        verify(sttClient).startStreaming("1");
        verify(sessionRepository).save(any(DebateSession.class));
        assertThat(attributes).containsEntry("debateId", "1");

        WebSocketMessage sent = captureSentMessage();
        assertThat(sent.type()).isEqualTo(MessageType.DEBATE_START);
        assertThat(sent.debateId()).isEqualTo(1L);
    }

    @Test
    void 바이너리_오디오를_받으면_해당_debateId로_오디오_청크를_전송한다() {
        attributes.put("debateId", "1");
        byte[] pcm = {1, 2, 3, 4};

        handler.handleBinaryMessage(connection, new BinaryMessage(pcm));

        verify(sttClient).sendAudioChunk(eq("1"), eq(pcm));
    }

    @Test
    void START_이전_바이너리는_무시한다() {
        handler.handleBinaryMessage(connection, new BinaryMessage(new byte[]{1}));

        verify(sttClient, never()).sendAudioChunk(any(), any());
    }

    @Test
    void STOP_제어메시지를_받으면_전사를_종료하고_DEBATE_END를_전송하며_세션을_삭제한다() throws Exception {
        DebateSession session = new DebateSession("1", connection);
        when(sessionRepository.deleteByDebateId("1")).thenReturn(Optional.of(session));

        handler.handleTextMessage(connection, new TextMessage("{\"type\":\"STOP\",\"sessionId\":\"1\"}"));

        verify(sttClient).stopStreaming("1");
        verify(sessionRepository).deleteByDebateId("1");

        WebSocketMessage sent = captureSentMessage();
        assertThat(sent.type()).isEqualTo(MessageType.DEBATE_END);
        assertThat(sent.debateId()).isEqualTo(1L);
    }

    @Test
    void 연결이_종료되면_남아있는_세션을_정리한다() {
        attributes.put("debateId", "1");
        when(sessionRepository.existsByDebateId("1")).thenReturn(true);
        when(sessionRepository.deleteByDebateId("1"))
                .thenReturn(Optional.of(new DebateSession("1", connection)));

        handler.afterConnectionClosed(connection, CloseStatus.NORMAL);

        verify(sttClient).stopStreaming("1");
    }

    private WebSocketMessage captureSentMessage() {
        ArgumentCaptor<WebSocketMessage> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(messageSender).send(any(DebateSession.class), captor.capture());
        return captor.getValue();
    }
}
