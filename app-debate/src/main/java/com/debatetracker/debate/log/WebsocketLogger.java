package com.debatetracker.debate.log;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebsocketLogger {

    private final MeterRegistry meterRegistry;

    public void recordSessionConnected() {
        meterRegistry.counter("ws.session.connected.count").increment();
    }

    public void recordSessionDisconnected() {
        meterRegistry.counter("ws.session.disconnected.count").increment();
    }

    public void recordSessionAbruptDisconnected() {
        meterRegistry.counter("ws.session.abrupt.count").increment();
    }

    public void recordInboundMessage(StompMessageType type) {
        meterRegistry.counter("ws.message.inbound.count", "type", type.getValue()).increment();
    }

    public void recordAudioChunkBytes(int bytes) {
        meterRegistry.counter("ws.audio.chunk.bytes").increment(bytes);
    }

    public void recordClientError(String errorCode) {
        meterRegistry.counter("ws.error.client.count", "error_code", errorCode).increment();
    }

    public void recordServerError(String errorCode) {
        meterRegistry.counter("ws.error.server.count", "error_code", errorCode).increment();
    }
}
