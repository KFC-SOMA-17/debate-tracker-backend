package com.debatetracker.infra.llm.chat.extract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
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
 * {@link ExtractLlmChat} 를 실제 Google GenAI(Gemini) API 키로 end-to-end 호출하는 수동 통합 테스트.
 *
 * <p>실 API 호출 비용·rate limit 이 발생하므로 평소 CI/로컬 빌드에서는 {@link Disabled} 로 항상 건너뛴다.
 * infra-llm/CLAUDE.md "테스트" 절 — 벤더 어댑터 통합 테스트는 별도 프로파일/수동 트리거 원칙을 따른다.
 *
 * <p>실행 방법:
 * <ol>
 *   <li>환경변수 {@code GOOGLE_API_KEY} 에 Gemini Developer API 키를 설정한다.</li>
 *   <li>아래 클래스 레벨 {@link Disabled} 어노테이션을 주석 처리하거나 제거한다.</li>
 *   <li>{@code .\gradlew :infra-llm:test --tests "*ExtractLlmChatApiTest"} 로 실행한다.</li>
 * </ol>
 *
 * <p>프로덕션 {@code LlmAutoConfiguration} 에 의존하지 않고, Spring AI 의 {@link ChatModel} 만 autoconfig 로
 * 생성한 뒤 프로덕션과 동일한 방식으로 {@link com.debatetracker.infra.llm.chat.ChatClientCaller} + {@link ExtractLlmChat} 를 직접 조립한다.
 * 프롬프트는 프로덕션과 동일한 {@code prompts/extract-*.txt} 클래스패스 리소스에서 주입받아
 * 프롬프트 품질까지 함께 검증한다.
 */
@Disabled("실 Google GenAI API 호출 비용이 발생하는 수동 통합 테스트 — GOOGLE_API_KEY 설정 후 @Disabled 를 제거해 실행한다")
@SpringBootTest(classes = ExtractLlmChatApiTest.RealApiConfig.class)
@TestPropertySource(properties = {
        "spring.ai.model.chat=google-genai",
        "spring.ai.google.genai.api-key=${GOOGLE_API_KEY:}",
        "spring.ai.google.genai.chat.options.model=gemini-2.5-pro",
        "spring.ai.google.genai.chat.options.temperature=0.0",
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
            LlmChatLogger logger = org.mockito.Mockito.mock(LlmChatLogger.class,
                org.mockito.Mockito.RETURNS_DEEP_STUBS);
            org.mockito.Mockito.doAnswer(invocation -> invocation.getArgument(1, java.util.function.Supplier.class).get())
                    .when(logger).executeWithMetrics(org.mockito.ArgumentMatchers.any(LlmOperationType.class), org.mockito.ArgumentMatchers.any());
            com.debatetracker.infra.llm.chat.ChatClientCaller caller =
                    new com.debatetracker.infra.llm.chat.ChatClientCaller(
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
            GoogleGenAiChatAutoConfiguration.class,
    })
    static class RealApiConfig {
    }
}
