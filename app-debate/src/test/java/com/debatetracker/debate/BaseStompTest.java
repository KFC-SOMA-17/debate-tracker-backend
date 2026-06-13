package com.debatetracker.debate;

import com.debatetracker.debate.config.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@Import(TestcontainersConfiguration.class)
@ExtendWith(DatabaseCleaner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseStompTest {

    private static final String STOMP_ENDPOINT = "/ws";

    protected StompSession stompSession;

    @LocalServerPort
    private int port;

    private final WebSocketStompClient websocketClient;

    protected BaseStompTest() {
        this.websocketClient = new WebSocketStompClient(new StandardWebSocketClient());
        this.websocketClient.setMessageConverter(buildMessageConverter());
    }

    @BeforeEach
    void connect() throws ExecutionException, InterruptedException, TimeoutException {
        this.stompSession = websocketClient
                .connectAsync("ws://localhost:" + port + STOMP_ENDPOINT, new StompSessionHandlerAdapter() {})
                .get(3, TimeUnit.SECONDS);
    }

    @AfterEach
    void disconnect() {
        if (stompSession.isConnected()) {
            stompSession.disconnect();
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
