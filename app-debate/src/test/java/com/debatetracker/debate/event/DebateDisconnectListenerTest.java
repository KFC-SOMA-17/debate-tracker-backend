현package com.debatetracker.debate.event;

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
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.debate.ws.session.BroadcasterReconnectGrace;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

class DebateDisconnectListenerTest {

    private BroadcasterReconnectGrace reconnectGrace;
    private DebateStreamingService debateStreamingService;
    private WebSocketMessageSender messageSender;
    private DebateDisconnectListener listener;

    @BeforeEach
    void setUp() {
        reconnectGrace = mock(BroadcasterReconnectGrace.class);
        debateStreamingService = mock(DebateStreamingService.class);
        messageSender = mock(WebSocketMessageSender.class);
        listener = new DebateDisconnectListener(reconnectGrace, debateStreamingService, messageSender);
    }

    private SessionDisconnectEvent disconnectEvent(String debateId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        accessor.setSessionId("ws-1");
        Map<String, Object> sessionAttributes = new HashMap<>();
        if (debateId != null) {
            sessionAttributes.put("debateId", debateId);
        }
        accessor.setSessionAttributes(sessionAttributes);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(this, message, "ws-1", CloseStatus.NORMAL);
    }

    private Runnable captureTermination(String debateId) {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(reconnectGrace).scheduleTermination(eq(debateId), captor.capture());
        return captor.getValue();
    }

    @Nested
    class OnDisconnect {

        @Test
        void 진행_중인_토론_세션이_종료되면_즉시_정리하지_않고_재연결_유예를_예약한다() {
            String debateId = "1";

            listener.onDisconnect(disconnectEvent(debateId));

            assertAll(
                    () -> verify(reconnectGrace).scheduleTermination(eq(debateId), any(Runnable.class)),
                    () -> verify(debateStreamingService, never()).stopDebateWithRemainingRefine(any()),
                    () -> verify(messageSender, never()).broadcast(any(), any())
            );
        }

        @Test
        void 토론과_무관한_세션이_종료되면_유예를_예약하지_않는다() {
            listener.onDisconnect(disconnectEvent(null));

            verify(reconnectGrace, never()).scheduleTermination(any(), any());
        }
    }

    @Nested
    class GraceExpiry {

        @Test
        void 유예_만료시_토론을_정리하고_DEBATE_END를_broadcast한다() {
            String debateId = "1";
            long debateIdValue = 1L;
            when(debateStreamingService.stopDebateWithRemainingRefine(debateId)).thenReturn(true);
            listener.onDisconnect(disconnectEvent(debateId));

            captureTermination(debateId).run();

            ArgumentCaptor<WebSocketMessage> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messageSender).broadcast(eq(debateId), captor.capture());
            WebSocketMessage sent = captor.getValue();
            assertAll(
                    () -> verify(debateStreamingService).stopDebateWithRemainingRefine(debateId),
                    () -> assertThat(sent.type()).isEqualTo(MessageType.DEBATE_END),
                    () -> assertThat(sent.debateId()).isEqualTo(debateIdValue)
            );
        }

        @Test
        void 유예_만료시_정리할_세션이_없으면_DEBATE_END를_broadcast하지_않는다() {
            String debateId = "1";
            when(debateStreamingService.stopDebateWithRemainingRefine(debateId)).thenReturn(false);
            listener.onDisconnect(disconnectEvent(debateId));

            captureTermination(debateId).run();

            assertAll(
                    () -> verify(debateStreamingService).stopDebateWithRemainingRefine(debateId),
                    () -> verify(messageSender, never()).broadcast(any(), any())
            );
        }
    }
}
