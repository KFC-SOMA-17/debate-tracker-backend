package com.debatetracker.debate.ws.sender;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.debatetracker.debate.domain.transcript.SpeechSegment;
import com.debatetracker.debate.ws.message.TranscriptionMessage;
import com.debatetracker.debate.ws.message.WebSocketMessage;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class WebSocketMessageSenderTest {

    private SimpMessagingTemplate messagingTemplate;
    private WebSocketMessageSender messageSender;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        messageSender = new WebSocketMessageSender(messagingTemplate);
    }

    @Nested
    class Broadcast {

        @Test
        void debateId로_topic_destination에_메시지를_convertAndSend한다() {
            String debateId = "1";
            WebSocketMessage message = new TranscriptionMessage(
                    1L,
                    new SpeechSegment("s1", "안녕하세요", "Guest_1", new BigDecimal("1.2"), new BigDecimal("4.8")));

            messageSender.broadcast(debateId, message);

            verify(messagingTemplate).convertAndSend("/topic/debate/1", message);
        }
    }
}
