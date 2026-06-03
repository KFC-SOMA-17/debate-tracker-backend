package com.debatetracker.debate.exception;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

/**
 * 위임 핸들러의 메시지 처리(text/binary)에서 새어 나온 예외를 가로채 WebSocketExceptionHandler 로 보낸다.
 * 예외가 밖으로 전파돼 Spring 이 연결을 SERVER_ERROR(1011)로 강제 종료하는 대신, 클라이언트는 ErrorMessage 를 받는다.
 */
public class ExceptionHandlingWebSocketHandler extends WebSocketHandlerDecorator {

    private final WebSocketExceptionHandler exceptionHandler;

    public ExceptionHandlingWebSocketHandler(WebSocketHandler delegate, WebSocketExceptionHandler exceptionHandler) {
        super(delegate);
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            super.handleMessage(session, message);
        } catch (Throwable e) {
            exceptionHandler.handle(session, e);
        }
    }
}
