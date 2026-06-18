package com.debatetracker.infra.llm.chat.refine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.infra.llm.chat.ChatClientCaller;
import com.debatetracker.infra.llm.client.RefineRequest;
import com.debatetracker.infra.llm.client.RefineResponse;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import com.debatetracker.infra.llm.log.LlmChatLogger;
import com.debatetracker.infra.llm.log.LlmOperationType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class RefineLlmChatTest {

    private static final String SYSTEM_PROMPT = "응답 형식: <RESPONSE_JSON_FORMAT>";
    private static final String USER_PROMPT = "맥락: <CONTEXTS>\n대상: <TARGETS>";


    @Nested
    class ProcessSystemPrompt {

        @Test
        void 응답_형식_토큰을_응답_스키마로_치환한다() {
            RefineLlmChat chat = newChat(mock(ChatClient.class, RETURNS_DEEP_STUBS));

            String prompt = chat.processSystemPrompt(request(List.of(segment("1", "A", "원본"))));

            assertAll(
                () -> assertThat(prompt).doesNotContain("<RESPONSE_JSON_FORMAT>"),
                () -> assertThat(prompt).contains("segments"),
                () -> assertThat(prompt).contains("id", "speaker", "text")
            );
        }
    }

    @Nested
    class ProcessUserPrompt {

        @Test
        void 맥락과_대상_토큰을_치환한다() {
            RefineLlmChat chat = newChat(mock(ChatClient.class, RETURNS_DEEP_STUBS));
            RefineRequest request = request(
                List.of(segment("ctx-1", "A", "맥락발화")),
                List.of(segment("tgt-1", "B", "대상발화")));

            String prompt = chat.processUserPrompt(request);

            assertAll(
                () -> assertThat(prompt).doesNotContain("<CONTEXTS>", "<TARGETS>"),
                () -> assertThat(prompt).contains("ctx-1", "맥락발화"),
                () -> assertThat(prompt).contains("tgt-1", "대상발화")
            );
        }

        @Test
        void 타임스탬프는_프롬프트에_포함하지_않는다() {
            RefineLlmChat chat = newChat(mock(ChatClient.class, RETURNS_DEEP_STUBS));
            TranscriptSegment target = new TranscriptSegment(
                "1", "A", new BigDecimal("1.680"), new BigDecimal("3.540"), "발화");

            String prompt = chat.processUserPrompt(request(List.of(target)));

            assertThat(prompt).doesNotContain("1.680", "3.540");
        }

        @Test
        void 맥락이_null이면_빈_배열로_직렬화한다() {
            RefineLlmChat chat = newChat(mock(ChatClient.class, RETURNS_DEEP_STUBS));
            RefineRequest request = new RefineRequest("s1", "주제", null,
                List.of(segment("1", "A", "발화")));

            String prompt = chat.processUserPrompt(request);

            assertThat(prompt).contains("\"segments\":[]");
        }
    }

    @Nested
    class Fetch {

        @Test
        void 정상_응답이면_정제된_텍스트를_적용한다() {
            RefineLlmChat chat = chatReturning("""
                {"segments":[
                  {"id":"1","speaker":"A","text":"정제1"},
                  {"id":"2","speaker":"B","text":"정제2"}
                ]}
                """);
            RefineRequest request = request(
                List.of(segment("1", "A", "원본1"), segment("2", "B", "원본2")));

            RefineResponse response = chat.fetch(request);

            assertAll(
                () -> assertThat(response.segments()).hasSize(2),
                () -> assertThat(response.segments().get(0).id()).isEqualTo("1"),
                () -> assertThat(response.segments().get(0).text()).isEqualTo("정제1"),
                () -> assertThat(response.segments().get(1).text()).isEqualTo("정제2")
            );
        }

        @Test
        void 타임스탬프_골격을_보존한다() {
            RefineLlmChat chat = chatReturning(
                "{\"segments\":[{\"id\":\"1\",\"speaker\":\"C\",\"text\":\"정제됨\"}]}");
            TranscriptSegment original = new TranscriptSegment(
                "1", "A", new BigDecimal("1.5"), new BigDecimal("2.5"), "원본");

            TranscriptSegment refined = chat.fetch(request(List.of(original))).segments()
                .getFirst();

            assertAll(
                () -> assertThat(refined.id()).isEqualTo("1"),
                () -> assertThat(refined.start()).isEqualByComparingTo(original.start()),
                () -> assertThat(refined.end()).isEqualByComparingTo(original.end()),
                () -> assertThat(refined.text()).isEqualTo("정제됨"),
                () -> assertThat(refined.speaker()).isEqualTo("C")
            );
        }

        @Test
        void 코드_펜스로_감싼_JSON을_파싱한다() {
            RefineLlmChat chat = chatReturning("""
                ```json
                {"segments":[{"id":"1","speaker":"A","text":"정제"}]}
                ```
                """);

            RefineResponse response = chat.fetch(request(List.of(segment("1", "A", "원본"))));

            assertThat(response.segments().getFirst().text()).isEqualTo("정제");
        }

        @Test
        void 응답_세그먼트_개수가_다르면_예외를_던진다() {
            RefineLlmChat chat = chatReturning(
                "{\"segments\":[{\"id\":\"1\",\"speaker\":\"A\",\"text\":\"정제\"}]}");
            RefineRequest request = request(
                List.of(segment("1", "A", "원본1"), segment("2", "A", "원본2")));

            assertThatThrownBy(() -> chat.fetch(request))
                .isInstanceOf(DebateTrackerException.class)
                .hasMessage("정제 응답의 세그먼트 개수가 요청과 일치하지 않습니다.");
        }

        @Test
        void 응답_세그먼트_순서가_뒤바뀌면_예외를_던진다() {
            RefineLlmChat chat = chatReturning("""
                {"segments":[
                  {"id":"2","speaker":"A","text":"정제2"},
                  {"id":"1","speaker":"A","text":"정제1"}
                ]}
                """);
            RefineRequest request = request(
                List.of(segment("1", "A", "원본1"), segment("2", "A", "원본2")));

            assertThatThrownBy(() -> chat.fetch(request))
                .isInstanceOf(DebateTrackerException.class)
                .hasMessage("정제 응답의 세그먼트 ID 가 요청과 일치하지 않습니다.");
        }

        @Test
        void 존재하지_않는_id가_응답되면_예외를_던진다() {
            RefineLlmChat chat = chatReturning(
                "{\"segments\":[{\"id\":\"99\",\"speaker\":\"A\",\"text\":\"정제\"}]}");
            RefineRequest request = request(List.of(segment("1", "A", "원본")));

            assertThatThrownBy(() -> chat.fetch(request))
                .isInstanceOf(DebateTrackerException.class)
                .hasMessage("정제 응답의 세그먼트 ID 가 요청과 일치하지 않습니다.");
        }

        @Test
        void JSON이_아닌_응답이면_예외를_던진다() {
            RefineLlmChat chat = chatReturning("이건 JSON이 아니다");

            assertThatThrownBy(() -> chat.fetch(request(List.of(segment("1", "A", "원본")))))
                .isInstanceOf(DebateTrackerException.class)
                .hasMessage("역직렬화에 실패했습니다.");
        }
    }

    private RefineLlmChat chatReturning(String cannedResponse) {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse()
            .getResult().getOutput().getText())
            .willReturn(cannedResponse);
        return newChat(chatClient);
    }

    private RefineLlmChat newChat(ChatClient chatClient) {
        given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse()
            .getMetadata().getUsage())
            .willReturn(null);
        given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse()
            .getResult().getMetadata().getFinishReason())
            .willReturn(null);
        LlmChatLogger logger = mock(LlmChatLogger.class, RETURNS_DEEP_STUBS);
        doAnswer(invocation -> invocation.getArgument(1, java.util.function.Supplier.class).get())
                .when(logger).executeWithMetrics(any(LlmOperationType.class), any());
        ChatClientCaller caller = new ChatClientCaller(chatClient, logger, LlmOperationType.REFINE);
        return new RefineLlmChat(caller, SYSTEM_PROMPT, USER_PROMPT);
    }

    private static RefineRequest request(List<TranscriptSegment> targets) {
        return request(List.of(), targets);
    }

    private static RefineRequest request(List<TranscriptSegment> contexts,
        List<TranscriptSegment> targets) {
        return new RefineRequest("session-1", "주제", contexts, targets);
    }

    private static TranscriptSegment segment(String id, String speaker, String text) {
        return new TranscriptSegment(id, speaker, new BigDecimal("0.0"), new BigDecimal("1.0"),
            text);
    }
}
