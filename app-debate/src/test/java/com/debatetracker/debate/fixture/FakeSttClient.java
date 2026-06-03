package com.debatetracker.debate.fixture;

import com.debatetracker.infra.stt.client.SttClient;
import com.debatetracker.infra.stt.client.dto.SttSegment;
import com.debatetracker.infra.stt.client.event.TranscribeEvent;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
public class FakeSttClient implements SttClient {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void startStreaming(String sessionId) {

    }

    @Override
    public void stopStreaming(String sessionId) {

    }

    @Override
    public void sendAudioChunk(String sessionId, byte[] pcmData) {
        TranscribeEvent event = new TranscribeEvent(
                sessionId,
                new SttSegment(
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        "S1",
                        "test content"
                )
        );
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public boolean isConnected(String sessionId) {
        return true;
    }
}
