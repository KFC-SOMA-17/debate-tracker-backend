package com.debatetracker.debate.exception;

import com.debatetracker.exception.ErrorCode;

public record ErrorResponse(ErrorCode errorCode, String message) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode, errorCode.getMessage());
    }
}
