package com.debatetracker.debate.ws.message;

import com.debatetracker.debate.exception.ErrorResponse;
import com.debatetracker.exception.ErrorCode;

/**
 * WebSocket 처리 중 발생한 예외를 클라이언트로 알리는 메시지. HTTP 에러와 동일한 {code, status, message}
 * 포맷(ErrorResponse)을 data 로 실어 일관성을 유지한다.
 */
public class ErrorMessage extends WebSocketMessage {

    private final ErrorResponse error;

    public ErrorMessage(long debateId, ErrorCode code) {
        super(debateId, MessageType.ERROR);
        this.error = ErrorResponse.from(code);
    }

    @Override
    public ErrorResponse data() {
        return error;
    }
}
