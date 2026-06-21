package com.debatetracker.debate.event;

import com.debatetracker.debate.infrastructure.config.AsyncConfig;
import com.debatetracker.debate.service.debate.DebateStreamingService;
import com.debatetracker.debate.ws.message.DebateEndMessage;
import com.debatetracker.debate.ws.sender.WebSocketMessageSender;
import com.debatetracker.debate.ws.session.BroadcasterReconnectGrace;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebateDisconnectListener {

    private final BroadcasterReconnectGrace reconnectGrace;
    private final DebateStreamingService debateStreamingService;
    private final WebSocketMessageSender messageSender;

    @Async(AsyncConfig.EVENT_LISTENER_EXECUTOR)
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        debateIdOf(event).ifPresentOrElse(
                this::scheduleTermination,
                () -> log.debug("토론과 무관한 세션 종료 — 무시: simpSessionId={}", event.getSessionId())
        );
    }

    private Optional<String> debateIdOf(SessionDisconnectEvent event) {
        Map<String, Object> sessionAttributes = SimpMessageHeaderAccessor.wrap(event.getMessage())
                .getSessionAttributes();
        return Optional.ofNullable(sessionAttributes)
                .map(attributes -> (String) attributes.get("debateId"));
    }

    private void scheduleTermination(String debateId) {
        log.info("진행자 연결 종료 감지 — 재연결 유예 시작: debateId={}", debateId);
        reconnectGrace.scheduleTermination(debateId, () -> terminate(debateId));
    }

    private void terminate(String debateId) {
        log.info("재연결 유예 만료 — 토론 정리: debateId={}", debateId);
        if (debateStreamingService.stopDebateWithRemainingRefine(debateId)) {
            messageSender.broadcast(debateId, new DebateEndMessage(Long.parseLong(debateId)));
        }
    }
}
