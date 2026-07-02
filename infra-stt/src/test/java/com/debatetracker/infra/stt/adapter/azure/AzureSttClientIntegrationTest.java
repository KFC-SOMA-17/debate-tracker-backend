package com.debatetracker.infra.stt.adapter.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.debatetracker.infra.stt.client.dto.SttSegment;
import com.debatetracker.infra.stt.client.event.TranscribeEvent;
import com.debatetracker.infra.stt.client.SttClient;
import com.debatetracker.infra.stt.config.AudioProperties;
import com.debatetracker.infra.stt.config.SttAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Disabled("실제 Azure 연동 테스트 — application-test.yml 설정 및 test-audio.pcm 준비 후 수동 실행")
@SpringBootTest(classes = {SttAutoConfiguration.class,
    AzureSttClientIntegrationTest.TranscribeEventCollector.class})
@ActiveProfiles("test")
class AzureSttClientIntegrationTest {

    @Autowired
    private SttClient adapter;

    @Autowired
    private AudioProperties audioProperties;

    @Autowired
    private TranscribeEventCollector eventCollector;

    @MockitoBean
    private MeterRegistry metaRegistry;

    @Nested
    class Streaming {

        @TestFactory
        Stream<DynamicTest> 스트리밍을_시작하고_음성을_전사한_뒤_종료할_수_있다() {
            String sessionId = "integration-test";

            return Stream.of(
                dynamicTest("스트리밍을 시작한다", () -> {
                    adapter.startStreaming(sessionId);

                    assertThat(adapter.isConnected(sessionId)).isTrue();
                }),
                dynamicTest("음성 청크를 전송하여 전사한다", () -> {
                    byte[] audioData = Files.readAllBytes(
                        Path.of("src/test/resources/test-audio.pcm"));
                    int chunkSize = audioProperties.chunkSizeBytes();

                    for (int i = 0; i < audioData.length; i += chunkSize) {
                        byte[] chunk = Arrays.copyOfRange(
                            audioData, i, Math.min(i + chunkSize, audioData.length));
                        adapter.sendAudioChunk(sessionId, chunk);
                        Thread.sleep(audioProperties.chunkDurationMs());
                    }

                    eventCollector.latch.await(5, TimeUnit.SECONDS);

                    List<SttSegment> results = eventCollector.segments;
                    assertThat(results).isNotEmpty();
                    results.forEach(segment ->
                        assertAll(
                            () -> assertThat(segment.content()).isNotBlank(),
                            () -> assertThat(segment.speaker()).isNotNull(),
                            () -> assertThat(segment.start()).isNotNull(),
                            () -> assertThat(segment.end()).isGreaterThan(segment.start())
                        )
                    );
                }),
                dynamicTest("스트리밍을 종료한다", () -> {
                    adapter.stopStreaming(sessionId);

                    assertThat(adapter.isConnected(sessionId)).isFalse();
                })
            );
        }
    }

    @Component
    static class TranscribeEventCollector {

        final List<SttSegment> segments = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        @EventListener
        void onTranscribe(TranscribeEvent event) {
            segments.add(event.segment());
            latch.countDown();
        }
    }
}
