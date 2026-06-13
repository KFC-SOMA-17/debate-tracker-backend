package com.debatetracker.debate;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

public class MessageFrameHandler<T> implements StompFrameHandler {

    private final CompletableFuture<T> completableFuture = new CompletableFuture<>();
    private final Class<T> type;

    public MessageFrameHandler(Class<T> type) {
        this.type = type;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleFrame(StompHeaders headers, Object payload) {
        completableFuture.complete((T) payload);
    }

    public CompletableFuture<T> getCompletableFuture() {
        return completableFuture;
    }
}
