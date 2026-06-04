package com.debatetracker.debate.exception;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

class ExceptionHandlingWebSocketHandlerTest {

    private WebSocketHandler delegate;
    private WebSocketExceptionHandler exceptionHandler;
    private ExceptionHandlingWebSocketHandler handler;

    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        delegate = mock(WebSocketHandler.class);
        exceptionHandler = mock(WebSocketExceptionHandler.class);
        handler = new ExceptionHandlingWebSocketHandler(delegate, exceptionHandler);

        session = mock(WebSocketSession.class);
    }

    @Nested
    class HandleMessage {

        @Test
        void delegate가_던진_예외는_밖으로_새지않고_ExceptionHandler로_전달된다() throws Exception {
            TextMessage message = new TextMessage("{}");
            RuntimeException error = new RuntimeException("boom");
            doThrow(error).when(delegate).handleMessage(session, message);

            assertAll(
                    () -> assertThatCode(() -> handler.handleMessage(session, message)).doesNotThrowAnyException(),
                    () -> verify(exceptionHandler).handle(eq(session), eq(error))
            );
        }

        @Test
        void 예외가_없으면_ExceptionHandler를_호출하지않고_정상_위임한다() throws Exception {
            TextMessage message = new TextMessage("{}");

            handler.handleMessage(session, message);

            verify(delegate).handleMessage(session, message);
        }
    }
}
