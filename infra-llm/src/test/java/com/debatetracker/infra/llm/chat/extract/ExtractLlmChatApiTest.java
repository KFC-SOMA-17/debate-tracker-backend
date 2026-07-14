package com.debatetracker.infra.llm.chat.extract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.infra.llm.chat.ChatClientCaller;
import com.debatetracker.infra.llm.client.ExtractAgenda;
import com.debatetracker.infra.llm.client.ExtractAgendaRequest;
import com.debatetracker.infra.llm.client.ExtractAgendaResponse;
import com.debatetracker.infra.llm.client.ExtractClaim;
import com.debatetracker.infra.llm.client.ExtractStance;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import com.debatetracker.infra.llm.log.LlmChatLogger;
import com.debatetracker.infra.llm.log.LlmOperationType;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
 * {@link ExtractLlmChat} 실 AWS Bedrock API end-to-end 수동 통합 테스트.
 *
 * <p>CI/로컬 빌드에서는 {@link Disabled} 로 항상 건너뛴다. 실행하려면:
 * <ol>
 *   <li>환경변수 {@code AWS_ACCESS_KEY_ID}, {@code AWS_SECRET_ACCESS_KEY}, {@code AWS_REGION} 설정</li>
 *   <li>클래스 레벨 {@link Disabled} 제거</li>
 *   <li>{@code .\gradlew :infra-llm:test --tests "*ExtractLlmChatApiTest"} 실행</li>
 * </ol>
 *
 * <p>{@code LlmAutoConfiguration} 없이 Spring AI {@link ChatModel} autoconfig 만으로
 * {@link com.debatetracker.infra.llm.chat.ChatClientCaller} + {@link ExtractLlmChat} 를 직접 조립한다.
 * {@code prompts/extract-*.txt} 를 그대로 주입해 프롬프트 품질도 함께 검증한다.
 */
@Disabled("실 AWS Bedrock API 호출 비용이 발생하는 수동 통합 테스트 — AWS 자격증명 설정 후 @Disabled 를 제거해 실행한다")
@SpringBootTest(classes = ExtractLlmChatApiTest.RealApiConfig.class)
@TestPropertySource(properties = {
    "spring.ai.model.chat=bedrock-converse",
    "spring.ai.bedrock.converse.chat.options.model=amazon.nova-pro-v1:0",
    "spring.ai.bedrock.converse.chat.options.temperature=0.0",
    "spring.ai.bedrock.converse.aws.region=${AWS_REGION:us-east-1}",
    "spring.ai.bedrock.converse.aws.accessKey=${AWS_ACCESS_KEY_ID:}",
    "spring.ai.bedrock.converse.aws.secretKey=${AWS_SECRET_ACCESS_KEY:}",
})
class ExtractLlmChatApiTest {

    @Autowired
    private ChatModel chatModel;

    @Value("classpath:prompts/extract-system.txt")
    private Resource systemPrompt;

    @Value("classpath:prompts/extract-user.txt")
    private Resource userPrompt;

    @Nested
    class Fetch {

        @Test
        void 실제_API로_발화에서_쟁점_트리를_추출한다() throws IOException {
            LlmChatLogger logger = Mockito.mock(LlmChatLogger.class, Mockito.RETURNS_DEEP_STUBS);
            Mockito.doAnswer(invocation -> invocation.getArgument(1, Supplier.class).get())
                .when(logger).executeWithMetrics(ArgumentMatchers.any(LlmOperationType.class),
                    ArgumentMatchers.any());
            ChatClientCaller caller = new ChatClientCaller(
                ChatClient.create(chatModel), logger, LlmOperationType.EXTRACT);
            ExtractLlmChat extractLlmChat = new ExtractLlmChat(
                caller,
                systemPrompt.getContentAsString(StandardCharsets.UTF_8),
                userPrompt.getContentAsString(StandardCharsets.UTF_8));

            TranscriptSegment context1 = new TranscriptSegment(
                "ctx-1", "찬성 1", new BigDecimal("0.0"), new BigDecimal("6.0"),
                "AI 창작물도 사람의 창작물처럼 저작권으로 보호해야 합니다. 실제로 많은 작가들이 AI 도구를 활용하고 있습니다.");
            TranscriptSegment context2 = new TranscriptSegment(
                "ctx-2", "반대 1", new BigDecimal("6.2"), new BigDecimal("13.0"),
                "AI 창작물에 저작권을 주면 안 됩니다. 한 연구에 따르면 AI 생성물의 70%가 기존 작품을 모방한 것으로 나타났습니다.");
            ExtractAgendaRequest request = new ExtractAgendaRequest(
                "session-real-api", List.of(context1, context2), Collections.emptyList());

            ExtractAgendaResponse response = extractLlmChat.fetch(request);

            assertAll(
                () -> assertThat(response.agendas()).isNotEmpty(),
                () -> assertThat(response.agendas()).allSatisfy(agenda -> {
                    assertThat(agenda.content()).isNotBlank();
                    assertThat(agenda.claims()).isNotNull();
                    assertThat(agenda.claims()).allSatisfy(claim -> {
                        assertThat(claim.stance()).isNotNull();
                        assertThat(claim.content()).isNotBlank();
                        assertThat(claim.evidences()).isNotNull();
                        assertThat(claim.evidences()).allSatisfy(
                            evidence -> assertThat(evidence.type()).isNotNull());
                    });
                }),
                () -> assertThat(response.agendas())
                    .flatExtracting(ExtractAgenda::claims)
                    .extracting(ExtractClaim::stance)
                    .contains(ExtractStance.PROS, ExtractStance.CONS)
            );

            // 쟁점 추출 품질은 콘솔 출력으로 직접 눈으로 확인한다
            response.agendas().forEach(agenda -> {
                System.out.printf("[쟁점] %s%n", agenda.content());
                agenda.claims().forEach(claim -> {
                    System.out.printf("  (%s) %s%n", claim.stance(), claim.content());
                    claim.evidences().forEach(evidence -> System.out.printf(
                        "    - [%s] %s%n", evidence.type(), evidence.content()));
                });
            });
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
