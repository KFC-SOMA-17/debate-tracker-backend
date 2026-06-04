package com.debatetracker.debate.exception;

import com.debatetracker.debate.domain.session.DebateSession;
import com.debatetracker.debate.ws.message.ErrorMessage;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

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
        if (errorCode.is5XxError()) {
            log.error("WebSocket server error: code={}", errorCode, throwable);
            return;
        }
        log.warn("WebSocket client error: code={}", errorCode, throwable);
    }
}
