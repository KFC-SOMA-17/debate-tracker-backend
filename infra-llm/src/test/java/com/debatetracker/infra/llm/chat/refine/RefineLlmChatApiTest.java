package com.debatetracker.infra.llm.chat.refine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
 * {@link RefineLlmChat} 를 실제 Google GenAI(Gemini) API 키로 end-to-end 호출하는 수동 통합 테스트.
 *
 * <p>실 API 호출 비용·rate limit 이 발생하므로 평소 CI/로컬 빌드에서는 {@link Disabled} 로 항상 건너뛴다.
 * infra-llm/CLAUDE.md "테스트" 절 — 벤더 어댑터 통합 테스트는 별도 프로파일/수동 트리거 원칙을 따른다.
 *
 * <p>실행 방법:
 * <ol>
 *   <li>환경변수 {@code GOOGLE_API_KEY} 에 Gemini Developer API 키를 설정한다.</li>
 *   <li>아래 클래스 레벨 {@link Disabled} 어노테이션을 주석 처리하거나 제거한다.</li>
 *   <li>{@code .\gradlew :infra-llm:test --tests "*RefineLlmChatApiTest"} 로 실행한다.</li>
 * </ol>
 *
 * <p>프로덕션 {@code LlmAutoConfiguration} 에 의존하지 않고, Spring AI 의 {@link ChatModel} 만 autoconfig 로
 * 생성한 뒤 프로덕션과 동일한 방식으로 {@link RefineLlmSelector} + {@link RefineLlmChat} 를 직접 조립한다.
 * 프롬프트는 프로덕션과 동일한 {@code prompts/refine-*.txt} 클래스패스 리소스에서 주입받아
 * 프롬프트 품질까지 함께 검증한다.
 */
@Disabled("실 Google GenAI API 호출 비용이 발생하는 수동 통합 테스트 — GOOGLE_API_KEY 설정 후 @Disabled 를 제거해 실행한다")
@SpringBootTest(classes = RefineLlmChatApiTest.RealApiConfig.class)
@TestPropertySource(properties = {
        "spring.ai.model.chat=google-genai",
        "spring.ai.google.genai.api-key=${GOOGLE_API_KEY}",
        "spring.ai.google.genai.chat.options.model=gemini-2.5-flash",
        "spring.ai.google.genai.chat.options.temperature=0.0",
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
            RefineLlmChat refineLlmChat = new RefineLlmChat(
                    new RefineLlmSelector(ChatClient.create(chatModel)),
                    systemPrompt.getContentAsString(StandardCharsets.UTF_8),
                    userPrompt.getContentAsString(StandardCharsets.UTF_8),
                    new ObjectMapper());

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
            GoogleGenAiChatAutoConfiguration.class,
    })
    static class RealApiConfig {
    }
}
