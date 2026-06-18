package com.debatetracker.debate.log;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
@RequiredArgsConstructor
public class StompMetricsListener implements ApplicationListener<AbstractSubProtocolEvent> {

    private final MeterRegistry meterRegistry;
    private final WebsocketLogger websocketLogger;

    private final AtomicInteger activeSessions = new AtomicInteger(0);
    private final AtomicInteger activeSubscriptions = new AtomicInteger(0);

    @PostConstruct
    public void registerGauges() {
        Gauge.builder("ws.session.active", activeSessions, AtomicInteger::get)
                .description("현재 활성 STOMP 세션 수")
                .register(meterRegistry);

        Gauge.builder("ws.subscription.active", activeSubscriptions, AtomicInteger::get)
                .description("현재 활성 STOMP 구독 수")
                .register(meterRegistry);
    }

    @Override
    public void onApplicationEvent(AbstractSubProtocolEvent event) {
        if (event instanceof SessionConnectedEvent) {
            activeSessions.incrementAndGet();
            websocketLogger.recordSessionConnected();

        } else if (event instanceof SessionDisconnectEvent disconnectEvent) {
            activeSessions.decrementAndGet();
            if (isAbruptDisconnect(disconnectEvent)) {
                websocketLogger.recordSessionAbruptDisconnected();
            } else {
                websocketLogger.recordSessionDisconnected();
            }

        } else if (event instanceof SessionSubscribeEvent) {
            activeSubscriptions.incrementAndGet();

        } else if (event instanceof SessionUnsubscribeEvent) {
            activeSubscriptions.decrementAndGet();
        }
    }

    private boolean isAbruptDisconnect(SessionDisconnectEvent event) {
        CloseStatus closeStatus = event.getCloseStatus();
        return closeStatus != null && !CloseStatus.NORMAL.equalsCode(closeStatus);
    }
}
