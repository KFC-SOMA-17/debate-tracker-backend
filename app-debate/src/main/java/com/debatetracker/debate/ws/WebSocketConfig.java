package com.debatetracker.debate.ws;

import com.debatetracker.debate.controller.config.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * /ws/stt WebSocket 설정. STT(Azure) 가 활성화된 경우에만 활성화된다 —
 * 비활성 시 SttClient 빈이 없으므로 핸들러를 만들 수 없고, 전사할 대상도 없다.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stt.azure.enabled", havingValue = "true")
public class WebSocketConfig implements WebSocketConfigurer {

    private static final int MAX_MESSAGE_BUFFER_SIZE = 64 * 1024;

    private final SttWebSocketHandler sttWebSocketHandler;
    private final CorsProperties corsProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sttWebSocketHandler, "/ws/stt")
                .setAllowedOriginPatterns(corsProperties.getOriginUrls());
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE);
        container.setMaxTextMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE);
        return container;
    }
}
