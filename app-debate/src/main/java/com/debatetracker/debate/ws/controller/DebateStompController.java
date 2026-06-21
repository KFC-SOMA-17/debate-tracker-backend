package com.debatetracker.debate.ws.controller;

import com.debatetracker.debate.log.DebateLogger;
import com.debatetracker.debate.log.annotation.LogStompAudio;
import com.debatetracker.debate.log.annotation.LogStompException;
import com.debatetracker.debate.log.annotation.LogStompStart;
import com.debatetracker.debate.log.annotation.LogStompStop;
import com.debatetracker.debate.service.debate.DebateStreamingService;
import com.debatetracker.debate.ws.message.DebateEndMessage;
import com.debatetracker.debate.ws.message.DebateStartMessage;
import com.debatetracker.debate.ws.message.ErrorMessage;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.debate.ws.session.BroadcasterReconnectGrace;
import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DebateStompController {

    private final DebateStreamingService debateStreamingService;
    private final WebSocketMessageSender messageSender;
    private final BroadcasterReconnectGrace reconnectGrace;
    private final DebateLogger debateLogger;

    @LogStompStart
    @MessageMapping("/debate/{debateId}/start")
    public void startDebate(@DestinationVariable String debateId, SimpMessageHeaderAccessor headerAccessor) {
        debateStreamingService.startDebate(debateId);
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("debateId", debateId); //세션에 진행 토론 기록
        }
        reconnectGrace.cancel(debateId); //재연결이면 예약된 종료 취소
        messageSender.broadcast(debateId, new DebateStartMessage(Long.parseLong(debateId)));
    }

    @LogStompStop
    @MessageMapping("/debate/{debateId}/stop")
    public void stopDebate(@DestinationVariable String debateId) {
        reconnectGrace.cancel(debateId);
        if (debateStreamingService.stopDebateWithRemainingRefine(debateId)) {
            debateLogger.recordStop();
            messageSender.broadcast(debateId, new DebateEndMessage(Long.parseLong(debateId)));
        } else {
            debateLogger.recordOrphan();
        }
    }

    @LogStompAudio
    @MessageMapping("/debate/{debateId}/audio")
    public void sendAudio(@DestinationVariable String debateId, @Payload byte[] payload) {
        debateStreamingService.sendAudioChunk(debateId, payload);
    }

    @LogStompException
    @MessageExceptionHandler
    public void handleException(@DestinationVariable String debateId, Throwable throwable) {
        ErrorCode errorCode = toErrorCode(throwable);
        messageSender.broadcast(debateId, new ErrorMessage(Long.parseLong(debateId), errorCode));
    }

    private ErrorCode toErrorCode(Throwable throwable) {
        if (throwable instanceof DebateTrackerException exception) {
            return exception.getErrorCode();
        }
        return ErrorCode.INTERNAL_SERVER_ERROR;
    }
}
