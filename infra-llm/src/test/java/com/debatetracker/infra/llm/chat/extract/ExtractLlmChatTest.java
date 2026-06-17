package com.debatetracker.infra.llm.chat.extract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import com.debatetracker.exception.DebateTrackerException;
import com.debatetracker.exception.ErrorCode;
import com.debatetracker.infra.llm.chat.ChatClientCaller;
import com.debatetracker.infra.llm.client.ExtractAgenda;
import com.debatetracker.infra.llm.client.ExtractAgendaRequest;
import com.debatetracker.infra.llm.client.ExtractAgendaResponse;
import com.debatetracker.infra.llm.client.ExtractStance;
import com.debatetracker.infra.llm.client.TranscriptSegment;
import com.debatetracker.infra.llm.log.LlmChatLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class ExtractLlmChatTest {

    private static final String SYSTEM_PROMPT =
            "응답 형식: <RESPONSE_JSON_FORMAT>\n입장: <STANCE_VALUES>\n근거유형: <EVIDENCE_TYPE_VALUES>";
    private static final String USER_PROMPT = "맥락: <CONTEXTS>\n이전 쟁점: <BEFORE_AGENDAS>";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    class ProcessSystemPrompt {

        @Test
        void 응답_형식과_enum_허용값_토큰을_치환한다() {
            ExtractLlmChat chat = newChat(mock(ChatClient.class, RETURNS_DEEP_STUBS));

            String prompt = chat.processSystemPrompt(request(List.of(segment("1", "A", "발화"))));

            assertAll(
                    () -> assertThat(prompt)
                            .doesNotContain("<RESPONSE_JSON_FORMAT>", "<STANCE_VALUES>", "<EVIDENCE_TYPE_VALUES>"),
                    () -> assertThat(prompt).contains("agendas", "stance", "content"),
                    () -> assertThat(prompt).contains("PROS", "CONS"),
                    () -> assertThat(prompt).contains("STATISTICS", "EXAMPLE", "QUOTATION")
            );
        }
    }

    @Nested
    class ProcessUserPrompt {

        @Test
        void 맥락과_이전_쟁점_토큰을_치환한다() {
            ExtractLlmChat chat = newChat(mock(ChatClient.class, RETURNS_DEEP_STUBS));
            ExtractAgendaRequest request = new ExtractAgendaRequest(
                    "s1",
                    List.of(segment("ctx-1", "A", "맥락발화")),
                    List.of(new ExtractAgenda("agenda-1", "이전쟁점", List.of())));

            String prompt = chat.processUserPrompt(request);

            assertAll(
                    () -> assertThat(prompt).doesNotContain("<CONTEXTS>", "<BEFORE_AGENDAS>"),
                    () -> assertThat(prompt).contains("맥락발화"),
                    () -> assertThat(prompt).contains("이전쟁점"),
                    () -> assertThat(prompt).contains("agenda-1")
            );
        }

        @Test
        void 맥락이_null이면_빈_배열로_직렬화한다() {
            ExtractLlmChat chat = newChat(mock(ChatClient.class, RETURNS_DEEP_STUBS));
            ExtractAgendaRequest request = new ExtractAgendaRequest("s1", null, null);

            String prompt = chat.processUserPrompt(request);

            assertThat(prompt).contains("맥락: []", "이전 쟁점: []");
        }
    }

    @Nested
    class Fetch {

        @Test
        void 정상_응답이면_쟁점_트리를_반환한다() {
            ExtractLlmChat chat = chatReturning("""
                    {"agendas":[
                      {"content":"쟁점1","claims":[
                        {"stance":"PROS","content":"찬성주장","evidences":[
                          {"type":"STATISTICS","content":"근거1"}]},
                        {"stance":"CONS","content":"반대주장","evidences":[]}
                      ]}
                    ]}
                    """);

            ExtractAgendaResponse response = chat.fetch(request(List.of(segment("1", "A", "발화"))));

            assertAll(
                    () -> assertThat(response.agendas()).hasSize(1),
                    () -> assertThat(response.agendas().getFirst().content()).isEqualTo("쟁점1"),
                    () -> assertThat(response.agendas().getFirst().claims()).hasSize(2),
                    () -> assertThat(response.agendas().getFirst().claims().getFirst().stance())
                            .isEqualTo(ExtractStance.PROS),
                    () -> assertThat(response.agendas().getFirst().claims().getFirst().evidences().getFirst().content())
                            .isEqualTo("근거1")
            );
        }

        @Test
        void 유지된_항목의_id는_보존하고_신규_항목의_id는_null로_파싱한다() {
            ExtractLlmChat chat = chatReturning("""
                    {"agendas":[
                      {"id":"a1","content":"유지된쟁점","claims":[
                        {"id":"c1","stance":"PROS","content":"유지된주장","evidences":[
                          {"id":"e1","type":"STATISTICS","content":"근거"}]}
                      ]},
                      {"id":null,"content":"새쟁점","claims":[
                        {"stance":"CONS","content":"새주장","evidences":[]}
                      ]}
                    ]}
                    """);

            ExtractAgendaResponse response = chat.fetch(request(List.of(segment("1", "A", "발화"))));

            ExtractAgenda kept = response.agendas().getFirst();
            ExtractAgenda added = response.agendas().get(1);
            assertAll(
                    () -> assertThat(kept.id()).isEqualTo("a1"),
                    () -> assertThat(kept.claims().getFirst().id()).isEqualTo("c1"),
                    () -> assertThat(kept.claims().getFirst().evidences().getFirst().id()).isEqualTo("e1"),
                    () -> assertThat(added.id()).isNull(),
                    () -> assertThat(added.claims().getFirst().id()).isNull()
            );
        }

        @Test
        void 코드_펜스로_감싼_JSON을_파싱한다() {
            ExtractLlmChat chat = chatReturning("""
                    ```json
                    {"agendas":[{"content":"쟁점","claims":[]}]}
                    ```
                    """);

            ExtractAgendaResponse response = chat.fetch(request(List.of(segment("1", "A", "발화"))));

            assertThat(response.agendas().getFirst().content()).isEqualTo("쟁점");
        }

        @Test
        void 잘못된_stance_값이면_파싱_예외를_던진다() {
            ExtractLlmChat chat = chatReturning(
                    "{\"agendas\":[{\"content\":\"쟁점\",\"claims\":["
                            + "{\"stance\":\"UNKNOWN\",\"content\":\"주장\",\"evidences\":[]}]}]}");

            assertThatThrownBy(() -> chat.fetch(request(List.of(segment("1", "A", "발화")))))
                    .isInstanceOf(DebateTrackerException.class)
                    .extracting(ex -> ((DebateTrackerException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LLM_RESPONSE_PARSING_FAILED);
        }

        @Test
        void 쟁점_content가_비면_형식_검증_예외를_던진다() {
            ExtractLlmChat chat = chatReturning(
                    "{\"agendas\":[{\"content\":\"  \",\"claims\":[]}]}");

            assertThatThrownBy(() -> chat.fetch(request(List.of(segment("1", "A", "발화")))))
                    .isInstanceOf(DebateTrackerException.class)
                    .extracting(ex -> ((DebateTrackerException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.EXTRACT_RESPONSE_INVALID_FORMAT);
        }

        @Test
        void 주장_stance가_null이면_형식_검증_예외를_던진다() {
            ExtractLlmChat chat = chatReturning(
                    "{\"agendas\":[{\"content\":\"쟁점\",\"claims\":["
                            + "{\"content\":\"주장\",\"evidences\":[]}]}]}");

            assertThatThrownBy(() -> chat.fetch(request(List.of(segment("1", "A", "발화")))))
                    .isInstanceOf(DebateTrackerException.class)
                    .extracting(ex -> ((DebateTrackerException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.EXTRACT_RESPONSE_INVALID_FORMAT);
        }

        @Test
        void JSON이_아닌_응답이면_파싱_예외를_던진다() {
            ExtractLlmChat chat = chatReturning("이건 JSON 이 아니다");

            assertThatThrownBy(() -> chat.fetch(request(List.of(segment("1", "A", "발화")))))
                    .isInstanceOf(DebateTrackerException.class)
                    .extracting(ex -> ((DebateTrackerException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.LLM_RESPONSE_PARSING_FAILED);
        }
    }

    private ExtractLlmChat chatReturning(String cannedResponse) {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getResult().getOutput().getText())
                .willReturn(cannedResponse);
        return newChat(chatClient);
    }

    private ExtractLlmChat newChat(ChatClient chatClient) {
        given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getMetadata().getUsage())
                .willReturn(null);
        given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getResult().getMetadata().getFinishReason())
                .willReturn(null);
        LlmChatLogger logger = mock(LlmChatLogger.class, RETURNS_DEEP_STUBS);
        ChatClientCaller caller = new ChatClientCaller(chatClient, logger, "extract");
        return new ExtractLlmChat(caller, SYSTEM_PROMPT, USER_PROMPT, objectMapper);
    }

    private static ExtractAgendaRequest request(List<TranscriptSegment> contexts) {
        return new ExtractAgendaRequest("session-1", contexts, List.of());
    }

    private static TranscriptSegment segment(String id, String speaker, String text) {
        return new TranscriptSegment(id, speaker, new BigDecimal("0.0"), new BigDecimal("1.0"), text);
    }
}
