package com.debatetracker.infra.llm.chat.refine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doAnswer;

import com.debatetracker.infra.llm.chat.ChatClientCaller;
import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import com.debatetracker.infra.llm.log.LlmChatLogger;
import com.debatetracker.infra.llm.log.LlmOperationType;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.bedrock.converse.autoconfigure.BedrockConverseProxyChatAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;

/**
 * {@link RefineLlmChat} 실 AWS Bedrock API end-to-end 수동 통합 테스트.
 *
 * <p>CI/로컬 빌드에서는 {@link Disabled} 로 항상 건너뛴다. 실행하려면:
 * <ol>
 *   <li>환경변수 {@code AWS_ACCESS_KEY_ID}, {@code AWS_SECRET_ACCESS_KEY}, {@code AWS_REGION} 설정</li>
 *   <li>클래스 레벨 {@link Disabled} 제거</li>
 *   <li>{@code .\gradlew :infra-llm:test --tests "*RefineLlmChatApiTest"} 실행</li>
 * </ol>
 *
 * <p>{@code LlmAutoConfiguration} 없이 Spring AI {@link ChatModel} autoconfig 만으로
 * {@link com.debatetracker.infra.llm.chat.ChatClientCaller} + {@link RefineLlmChat} 를 직접 조립한다.
 * {@code prompts/refine-*.txt} 를 그대로 주입해 프롬프트 품질도 함께 검증한다.
 */
@Disabled("실 AWS Bedrock API 호출 비용이 발생하는 수동 통합 테스트 — AWS 자격증명 설정 후 @Disabled 를 제거해 실행한다")
@SpringBootTest(classes = RefineLlmChatApiTest.RealApiConfig.class)
@TestPropertySource(properties = {
        "spring.ai.model.chat=bedrock-converse",
        "spring.ai.bedrock.converse.chat.options.model=us.amazon.nova-2-lite-v1:0",
        "spring.ai.bedrock.converse.chat.options.temperature=0.0",
        "spring.ai.bedrock.converse.aws.region=${AWS_REGION:us-east-1}",
        "spring.ai.bedrock.converse.aws.accessKey=${AWS_ACCESS_KEY_ID:}",
        "spring.ai.bedrock.converse.aws.secretKey=${AWS_SECRET_ACCESS_KEY:}",
})
class RefineLlmChatApiTest {

    @Autowired
    private ChatModel chatModel;

    @Value("classpath:prompts/refine-system.txt")
    private Resource systemPrompt;

    @Value("classpath:prompts/refine-user.txt")
    private Resource userPrompt;

    @Nested
    class Fetch {

        @Test
        void 실제_API로_타임스탬프_골격을_유지하며_대상을_정제한다() throws IOException {
            LlmChatLogger logger = org.mockito.Mockito.mock(LlmChatLogger.class, Mockito.RETURNS_DEEP_STUBS);
            doAnswer(invocation -> invocation.getArgument(1, java.util.function.Supplier.class).get())
                    .when(logger).executeWithMetrics(org.mockito.ArgumentMatchers.any(LlmOperationType.class), org.mockito.ArgumentMatchers.any());
            ChatClientCaller caller = new com.debatetracker.infra.llm.chat.ChatClientCaller(
                            ChatClient.create(chatModel), logger, LlmOperationType.REFINE);
            RefineLlmChat refineLlmChat = new RefineLlmChat(
                    caller,
                    systemPrompt.getContentAsString(StandardCharsets.UTF_8),
                    userPrompt.getContentAsString(StandardCharsets.UTF_8));

            TranscriptSegment context = new TranscriptSegment(
                    "ctx-1", "찬성 1", new BigDecimal("0.0"), new BigDecimal("4.2"),
                    "저는 인공지능 규제가 혁신을 저해한다고 생각합니다");
            TranscriptSegment target1 = new TranscriptSegment(
                    "tgt-1", "반대 1", new BigDecimal("4.5"), new BigDecimal("9.1"),
                    "인공지능 규재는 안전을 위해 반드시");
            TranscriptSegment target2 = new TranscriptSegment(
                    "tgt-2", "찬성 2", new BigDecimal("9.3"), new BigDecimal("13.0"),
                    "필요 합니다 아닙니다 그렇지 않습니다 이미 충분한 안저 문제 고려하고 있습니다");
            RefineRequest request = new RefineRequest(
                    "session-real-api", "인공지능 규제", List.of(context), List.of(target1, target2));

            RefineResponse response = refineLlmChat.fetch(request);

            assertAll(
                    () -> assertThat(response.segments()).hasSize(2),
                    () -> assertThat(response.segments())
                            .extracting(TranscriptSegment::id)
                            .containsExactly("tgt-1", "tgt-2"),
                    () -> assertThat(response.segments().get(0).start()).isEqualByComparingTo(target1.start()),
                    () -> assertThat(response.segments().get(0).end()).isEqualByComparingTo(target1.end()),
                    () -> assertThat(response.segments().get(1).start()).isEqualByComparingTo(target2.start()),
                    () -> assertThat(response.segments().get(1).end()).isEqualByComparingTo(target2.end()),
                    () -> assertThat(response.segments()).allSatisfy(segment -> assertThat(segment.text()).isNotBlank())
            );

            // 정제 품질은 콘솔 출력으로 직접 눈으로 확인한다
            response.segments().forEach(segment -> System.out.printf(
                    "[%s] %s : %s%n", segment.id(), segment.speaker(), segment.text()));
        }
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            JacksonAutoConfiguration.class,
            SpringAiRetryAutoConfiguration.class,
            ToolCallingAutoConfiguration.class,
            BedrockConverseProxyChatAutoConfiguration.class,
    })
    static class RealApiConfig {
    }
}
