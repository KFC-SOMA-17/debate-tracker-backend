package com.debatetracker.debate.ws.controller;

import com.debatetracker.debate.service.debate.DebateStreamingService;
import com.debatetracker.debate.ws.message.DebateEndMessage;
import com.debatetracker.debate.ws.message.DebateStartMessage;
import com.debatetracker.debate.ws.message.ErrorMessage;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DebateStompController {

    private final DebateStreamingService debateStreamingService;
    private final WebSocketMessageSender messageSender;

    @MessageMapping("/debate/{debateId}/start")
    public void startDebate(@DestinationVariable String debateId) {
        debateStreamingService.startDebate(debateId);
        messageSender.broadcast(debateId, new DebateStartMessage(Long.parseLong(debateId)));
    }

    @MessageMapping("/debate/{debateId}/stop")
    public void stopDebate(@DestinationVariable String debateId) {
        if (debateStreamingService.stopDebateWithRemainingRefine(debateId)) {
            messageSender.broadcast(debateId, new DebateEndMessage(Long.parseLong(debateId)));
        }
    }

    @MessageMapping("/debate/{debateId}/audio")
    public void sendAudio(@DestinationVariable String debateId, @Payload byte[] payload) {
        debateStreamingService.sendAudioChunk(debateId, payload);
    }

    @MessageExceptionHandler
    public void handleException(@DestinationVariable String debateId, Throwable throwable) {
        ErrorCode errorCode = toErrorCode(throwable);
        logBySeverity(debateId, errorCode, throwable);
        messageSender.broadcast(debateId, new ErrorMessage(Long.parseLong(debateId), errorCode));
    }

    private ErrorCode toErrorCode(Throwable throwable) {
        if (throwable instanceof DebateTrackerException exception) {
            return exception.getErrorCode();
        }
        return ErrorCode.INTERNAL_SERVER_ERROR;
    }

    private void logBySeverity(String debateId, ErrorCode errorCode, Throwable throwable) {
        if (errorCode.is5XxError()) {
            log.error("STOMP server error: debateId={}, code={}", debateId, errorCode, throwable);
            return;
        }
        log.warn("STOMP client error: debateId={}, code={}", debateId, errorCode, throwable);
    }
}
