package com.debatetracker.debate.ws.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.debatetracker.debate.exception.ErrorResponse;
import com.debatetracker.debate.service.debate.DebateStreamingService;
import com.debatetracker.debate.ws.message.MessageType;
import com.debatetracker.debate.ws.message.WebSocketMessage;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DebateStompControllerExceptionHandlerTest {

    private WebSocketMessageSender messageSender;
    private DebateStompController controller;

    @BeforeEach
    void setUp() {
        messageSender = mock(WebSocketMessageSender.class);
        controller = new DebateStompController(mock(DebateStreamingService.class), messageSender);
    }

    @Nested
    class HandleException {

        @Test
        void DebateTrackerException은_그_ErrorCode를_담은_ERROR를_topic으로_broadcast한다() {
            String debateId = "1";
            long debateIdValue = 1L;
            ErrorCode errorCode = ErrorCode.NOT_FOUND_DEBATE_ID;

            controller.handleException(debateId, new DebateTrackerException(errorCode));

            ArgumentCaptor<WebSocketMessage> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messageSender).broadcast(eq(debateId), captor.capture());
            WebSocketMessage message = captor.getValue();
            assertAll(
                    () -> assertThat(message.type()).isEqualTo(MessageType.ERROR),
                    () -> assertThat(message.debateId()).isEqualTo(debateIdValue),
                    () -> assertThat(((ErrorResponse) message.data()).code()).isEqualTo(errorCode)
            );
        }

        @Test
        void 일반_예외는_INTERNAL_SERVER_ERROR로_매핑해_broadcast한다() {
            String debateId = "1";

            controller.handleException(debateId, new RuntimeException("boom"));

            ArgumentCaptor<WebSocketMessage> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messageSender).broadcast(eq(debateId), captor.capture());
            assertThat(((ErrorResponse) captor.getValue().data()).code()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
