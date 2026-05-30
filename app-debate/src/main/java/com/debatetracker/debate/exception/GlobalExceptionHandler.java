package com.debatetracker.debate.exception;

import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException exception) {
        logClientError(exception);
        return toResponse(ErrorCode.FIELD_ERROR);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        logClientError(exception);
        return toResponse(ErrorCode.URL_PARAMETER_ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception) {
        logClientError(exception);
        return toResponse(ErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH);
    }

    @ExceptionHandler(ClientAbortException.class)
    public ResponseEntity<ErrorResponse> handleClientAbortException(ClientAbortException exception) {
        logClientError(exception);
        return toResponse(ErrorCode.ALREADY_DISCONNECTED);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<ErrorResponse> handleAsyncRequestNotUsableException(AsyncRequestNotUsableException exception) {
        if (isClientDisconnect(exception.getCause())) {
            logClientError(exception);
            return toResponse(ErrorCode.ALREADY_DISCONNECTED);
        }
        logServerError(exception);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception) {
        logClientError(exception);
        return toResponse(ErrorCode.METHOD_NOT_SUPPORTED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException exception) {
        logClientError(exception);
        return toResponse(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException exception) {
        logClientError(exception);
        return toResponse(ErrorCode.NO_RESOURCE_FOUND);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestCookieException(MissingRequestCookieException exception) {
        logClientError(exception);
        return toResponse(ErrorCode.NO_COOKIE_FOUND);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(MultipartException exception) {
        logClientError(exception);
        return toResponse(ErrorCode.FILE_UPLOAD_ERROR);
    }

    @ExceptionHandler(DebateTrackerException.class)
    public ResponseEntity<ErrorResponse> handleDebateTrackerException(DebateTrackerException exception) {
        logServerError(exception);
        return toResponse(exception.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        logServerError(exception);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private boolean isClientDisconnect(Throwable cause) {
        return cause instanceof IOException
                && cause.getMessage() != null
                && cause.getMessage().contains("Broken pipe");
    }

    private void logClientError(Exception exception) {
        log.warn("Client error exception message", exception);
    }

    private void logServerError(Exception exception) {
        log.error("Server error exception occurred", exception);
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
        ErrorResponse errorResponse = ErrorResponse.from(errorCode);
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(errorResponse);
    }
}
