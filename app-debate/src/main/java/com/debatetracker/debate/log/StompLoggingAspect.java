package com.debatetracker.debate.log;

import com.debatetracker.debate.log.annotation.LogStompAudio;
import com.debatetracker.debate.log.annotation.LogStompException;
import com.debatetracker.debate.log.annotation.LogStompStart;
import com.debatetracker.debate.log.annotation.LogStompStop;
import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class StompLoggingAspect {

    private final WebsocketLogger websocketLogger;
    private final DebateLogger debateLogger;

    @Before("@annotation(com.debatetracker.debate.log.annotation.LogStompStart)")
    public void recordStart(JoinPoint joinPoint) {
        websocketLogger.recordInboundMessage(StompMessageType.START);
        debateLogger.recordStart();
    }

    @Before("@annotation(com.debatetracker.debate.log.annotation.LogStompStop)")
    public void recordStop(JoinPoint joinPoint) {
        websocketLogger.recordInboundMessage(StompMessageType.STOP);
    }

    @Before("@annotation(com.debatetracker.debate.log.annotation.LogStompAudio)")
    public void recordAudio(JoinPoint joinPoint) {
        websocketLogger.recordInboundMessage(StompMessageType.AUDIO);
        byte[] payload = findPayloadArg(joinPoint.getArgs());
        if (payload != null) {
            websocketLogger.recordAudioChunkBytes(payload.length);
        }
    }

    @Before("@annotation(com.debatetracker.debate.log.annotation.LogStompException)")
    public void recordException(JoinPoint joinPoint) {
        Throwable throwable = findThrowableArg(joinPoint.getArgs());
        if (throwable == null) {
            return;
        }
        ErrorCode errorCode = toErrorCode(throwable);
        String debateId = findDestinationVariable(joinPoint);

        if (errorCode.is5XxError()) {
            websocketLogger.recordServerError(errorCode.name());
            log.error("STOMP server error: debateId={}, code={}", debateId, errorCode, throwable);
        } else {
            websocketLogger.recordClientError(errorCode.name());
            log.warn("STOMP client error: debateId={}, code={}", debateId, errorCode, throwable);
        }
    }

    private byte[] findPayloadArg(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof byte[] bytes) {
                return bytes;
            }
        }
        return null;
    }

    private Throwable findThrowableArg(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Throwable t) {
                return t;
            }
        }
        return null;
    }

    private String findDestinationVariable(JoinPoint joinPoint) {
        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(DestinationVariable.class) && args[i] instanceof String id) {
                    return id;
                }
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private ErrorCode toErrorCode(Throwable throwable) {
        if (throwable instanceof DebateTrackerException exception) {
            return exception.getErrorCode();
        }
        return ErrorCode.INTERNAL_SERVER_ERROR;
    }
}
