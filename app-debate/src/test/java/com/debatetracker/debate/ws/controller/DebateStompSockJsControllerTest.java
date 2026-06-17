package com.debatetracker.debate.ws.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.debate.MessageFrameHandler;
import com.debatetracker.debate.config.TestcontainersConfiguration;
import com.debatetracker.debate.ws.StompMessageResponse;
import com.debatetracker.debate.ws.message.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DebateStompSockJsControllerTest {

    private static final String SOCKJS_ENDPOINT = "/ws";

    @LocalServerPort
    private int port;

    private final WebSocketStompClient sockJsClient;

    private StompSession stompSession;

    DebateStompSockJsControllerTest() {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        this.sockJsClient = new WebSocketStompClient(new SockJsClient(transports));
        this.sockJsClient.setMessageConverter(buildMessageConverter());
    }

    @BeforeEach
    void connect() throws ExecutionException, InterruptedException, TimeoutException {
        this.stompSession = sockJsClient
                .connectAsync("http://localhost:" + port + SOCKJS_ENDPOINT, new StompSessionHandlerAdapter() {})
                .get(3, TimeUnit.SECONDS);
    }

    @AfterEach
    void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }

    @Nested
    class SockJsConnect {

        @Test
        void SockJS로_연결해_토픽을_구독하면_broadcast를_수신한다() throws Exception {
            String debateId = "1";
            long debateIdValue = 1L;
            MessageFrameHandler<StompMessageResponse> handler = new MessageFrameHandler<>(StompMessageResponse.class);

            stompSession.subscribe("/topic/debate/" + debateId, handler);
            stompSession.send("/app/debate/" + debateId + "/start", new byte[0]);

            StompMessageResponse response = handler.getCompletableFuture().get(3L, TimeUnit.SECONDS);
            assertAll(
                    () -> assertThat(response.type()).isEqualTo(MessageType.DEBATE_START),
                    () -> assertThat(response.debateId()).isEqualTo(debateIdValue)
            );
        }
    }

    private MessageConverter buildMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
        converter.setObjectMapper(objectMapper);
        return converter;
    }
}
