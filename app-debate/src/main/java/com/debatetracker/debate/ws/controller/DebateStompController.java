package com.debatetracker.debate.ws.controller;

import com.debatetracker.debate.service.debate.DebateStreamingService;
import com.debatetracker.debate.ws.message.DebateEndMessage;
import com.debatetracker.debate.ws.message.DebateStartMessage;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

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
        debateStreamingService.stopDebateWithRemainingRefine(debateId);
        messageSender.broadcast(debateId, new DebateEndMessage(Long.parseLong(debateId)));
    }
}
