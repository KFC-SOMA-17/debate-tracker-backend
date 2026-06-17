package com.debatetracker.debate.controller.config;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    private static final int MAX_MESSAGE_SIZE = 64 * 1024;
    private static final int MAX_SESSION_BUFFER_SIZE = 64 * 1024;
    private static final int MAX_SENT_TIMEOUT_LIMIT = Math.toIntExact(Duration.ofSeconds(10).toMillis());
    private static final long HEARTBEAT_INTERVAL_MS = 2_000L;
    private static final int HEARTBEAT_POOL_SIZE = 1;
    private static final String HEARTBEAT_THREAD_NAME_PREFIX = "ws-heartbeat-";

    private final CorsProperties corsProperties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(corsProperties.getOriginUrls());
        registry.addEndpoint("/ws")
                .setAllowedOrigins(corsProperties.getOriginUrls())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[] {HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS})
                .setTaskScheduler(webSocketHeartbeatScheduler());
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(MAX_MESSAGE_SIZE)
                .setSendBufferSizeLimit(MAX_SESSION_BUFFER_SIZE)
                .setSendTimeLimit(MAX_SENT_TIMEOUT_LIMIT);
    }

    @Bean
    public ThreadPoolTaskScheduler webSocketHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(HEARTBEAT_POOL_SIZE);
        scheduler.setThreadNamePrefix(HEARTBEAT_THREAD_NAME_PREFIX);
        return scheduler;
    }
}
