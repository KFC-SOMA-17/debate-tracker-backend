package com.debatetracker.exception;

import lombok.Getter;

@Getter
public class DebateTrackerException extends RuntimeException {

    private final ErrorCode errorCode;

    public DebateTrackerException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public DebateTrackerException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
