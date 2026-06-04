package com.debatetracker.debate.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.debate.ws.message.ErrorMessage;
import com.debatetracker.debate.ws.message.MessageType;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketSession;

class WebSocketExceptionHandlerTest {

    private WebSocketMessageSender messageSender;
    private WebSocketExceptionHandler exceptionHandler;

    private WebSocketSession session;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        messageSender = mock(WebSocketMessageSender.class);
        exceptionHandler = new WebSocketExceptionHandler(messageSender);

        session = mock(WebSocketSession.class);
        attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
    }

    @Nested
    class Handle {

        @Test
        void DebateTrackerException은_해당_ErrorCode로_ERROR메시지를_전송한다() {
            String debateId = "1";
            long debateIdValue = 1L;
            attributes.put("debateId", debateId);

            exceptionHandler.handle(session, new DebateTrackerException(ErrorCode.NOT_FOUND_DEBATE_ID));

            ErrorMessage sent = captureSentMessage();
            assertAll(
                    () -> assertThat(sent.type()).isEqualTo(MessageType.ERROR),
                    () -> assertThat(sent.debateId()).isEqualTo(debateIdValue),
                    () -> assertThat(sent.data().code()).isEqualTo(ErrorCode.NOT_FOUND_DEBATE_ID)
            );
        }

        @Test
        void 일반_예외는_INTERNAL_SERVER_ERROR로_매핑한다() {
            String debateId = "1";
            attributes.put("debateId", debateId);

            exceptionHandler.handle(session, new RuntimeException("boom"));

            ErrorResponse error = captureSentMessage().data();
            assertThat(error.code()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        @Test
        void session_attribute의_debateId로_ERROR메시지를_전송한다() {
            String debateId = "42";
            long debateIdValue = 42L;
            attributes.put("debateId", debateId);

            exceptionHandler.handle(session, new RuntimeException("boom"));

            assertThat(captureSentMessage().debateId()).isEqualTo(debateIdValue);
        }
    }

    private ErrorMessage captureSentMessage() {
        ArgumentCaptor<ErrorMessage> captor = ArgumentCaptor.forClass(ErrorMessage.class);
        verify(messageSender).send(eq(session), captor.capture());
        return captor.getValue();
    }
}
