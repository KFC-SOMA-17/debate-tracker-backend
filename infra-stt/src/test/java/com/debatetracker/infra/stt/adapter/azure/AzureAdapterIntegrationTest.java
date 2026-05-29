package com.debatetracker.infra.stt.adapter.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.debatetracker.infra.stt.config.AudioProperties;
import com.debatetracker.infra.stt.config.AzureConfig;
import com.debatetracker.infra.stt.config.SttAutoConfiguration;
import com.debatetracker.infra.stt.dto.SttSegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("실제 Azure 연동 테스트 — application-test.yml 설정 및 test-audio.pcm 준비 후 수동 실행")
@SpringBootTest(classes = SttAutoConfiguration.class)
@ActiveProfiles("test")
class AzureAdapterIntegrationTest {

    @Autowired
    private AzureConfig azureConfig;

    @Autowired
    private AudioProperties audioProperties;

    @DisplayName("스트리밍을 시작하고 음성을 전사한 뒤 종료할 수 있다")
    @TestFactory
    Stream<DynamicTest> 스트리밍_전사_시나리오() {
        String sessionId = "integration-test";
        AzureAdapter adapter = new AzureAdapter(azureConfig, audioProperties);
        List<SttSegment> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        return Stream.of(
                dynamicTest("스트리밍을 시작한다", () -> {
                    adapter.startStreaming(sessionId, segment -> {
                        results.add(segment);
//                        System.out.printf("[전사 결과] speaker=%s, start=%.2f, end=%.2f, content=%s%n",
//                                segment.speaker(), segment.start(), segment.end(), segment.content());
                        latch.countDown();
                    });

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

                    latch.await(5, TimeUnit.SECONDS);

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
