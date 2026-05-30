package com.debatetracker.debate.exception;

import com.debatetracker.exception.ErrorCode;

public record ErrorResponse(ErrorCode code, int status, String message) {

    public static ErrorResponse from(ErrorCode code) {
        return new ErrorResponse(code, code.getStatusCode(), code.getMessage());
    }
}
