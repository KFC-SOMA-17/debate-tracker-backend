package com.debatetracker.debate.ws.exception;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.ws.message.ErrorMessage;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket 메시지 처리 중 발생한 예외를 ErrorCode 로 매핑해 ErrorMessage 로 클라이언트에 전송한다.
 * HTTP 의 GlobalExceptionHandler(RestControllerAdvice)에 대응하는 WebSocket 측 중앙 예외 처리기.
 * RestControllerAdvice 는 DispatcherServlet 요청에만 적용되어 WebSocket 핸들러 예외는 잡지 못한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketExceptionHandler {

    private final WebSocketMessageSender messageSender;

    public void handle(WebSocketSession session, Throwable throwable) {
        DebateSession debateSession = new DebateSession(session);
        ErrorCode errorCode = toErrorCode(throwable);
        logBySeverity(errorCode, throwable);
        messageSender.send(
                debateSession.connection(),
                new ErrorMessage(Long.parseLong(debateSession.debateId()), errorCode)
        );
    }

    private ErrorCode toErrorCode(Throwable throwable) {
        if (throwable instanceof DebateTrackerException exception) {
            return exception.getErrorCode();
        }
        return ErrorCode.INTERNAL_SERVER_ERROR;
    }

    private void logBySeverity(ErrorCode errorCode, Throwable throwable) {
        if (errorCode.getStatusCode() >= 500) {
            log.error("WebSocket server error: code={}", errorCode, throwable);
            return;
        }
        log.warn("WebSocket client error: code={}", errorCode, throwable);
    }
}
