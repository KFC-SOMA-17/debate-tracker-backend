package com.debatetracker.infra.llm.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.debatetracker.infra.llm.log.LlmChatLogger;
import com.debatetracker.infra.llm.log.LlmOperationType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.client.ChatClient;

class LlmChatTest {

    private static ChatClientCaller createCaller(ChatClient chatClient) {
        LlmChatLogger logger = mock(LlmChatLogger.class, RETURNS_DEEP_STUBS);
        doAnswer(invocation -> invocation.getArgument(1, java.util.function.Supplier.class).get())
                .when(logger).executeWithMetrics(any(LlmOperationType.class), any());
        return new ChatClientCaller(chatClient, logger, LlmOperationType.REFINE);
    }

    @Nested
    class Fetch {

        @CsvSource(delimiter = '|', value = {
                "hello world                       | hello world  | 펜스 없으면 그대로(trim)",
                "'```json\n{\"a\":1}\n```'         | {\"a\":1}     | json 코드펜스 제거",
                "'```\n{\"a\":1}\n```'             | {\"a\":1}     | 언어 없는 코드펜스 제거",
                "'```json\n{\"a\":1}'              | {\"a\":1}     | 닫는 펜스 없으면 여는 펜스만 제거",
                "'```json'                         | '```json'    | 개행 없는 펜스는 원본 유지",
        })
        @ParameterizedTest(name = "[{index}] {2}")
        void 응답을_감싼_코드_펜스를_제거한다(String raw, String expected, String description) {
            ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
            given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getResult().getOutput().getText())
                    .willReturn(raw);
            given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getMetadata().getUsage())
                    .willReturn(null);
            given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getResult().getMetadata().getFinishReason())
                    .willReturn(null);
            EchoLlmChat chat = new EchoLlmChat(createCaller(chatClient));

            String result = chat.fetch("ignored");

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void 템플릿_단계를_정해진_순서대로_호출한다() {
            ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
            given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getResult().getOutput().getText())
                    .willReturn("raw");
            given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getMetadata().getUsage())
                    .willReturn(null);
            given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getResult().getMetadata().getFinishReason())
                    .willReturn(null);
            RecordingLlmChat chat = new RecordingLlmChat(createCaller(chatClient));

            chat.fetch("req");

            assertThat(chat.calls)
                    .containsExactly("processSystem", "processUser", "refineResponse", "validate");
        }

        @Test
        void 검증_실패를_그대로_전파한다() {
            ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
            given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getResult().getOutput().getText())
                    .willReturn("raw");
            given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getMetadata().getUsage())
                    .willReturn(null);
            given(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse().getResult().getMetadata().getFinishReason())
                    .willReturn(null);
            EchoLlmChat chat = new EchoLlmChat(createCaller(chatClient)) {
                @Override
                protected void validate(String request, String response) {
                    throw new IllegalStateException("invalid");
                }
            };

            assertThatThrownBy(() -> chat.fetch("req"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("invalid");
        }
    }

    private static class EchoLlmChat extends LlmChat<String, String> {

        EchoLlmChat(ChatClientCaller chatClientCaller) {
            super(chatClientCaller, "system", "user");
        }

        @Override
        protected String processSystemPrompt(String request) {
            return getSystemPrompt();
        }

        @Override
        protected String processUserPrompt(String request) {
            return getUserPrompt();
        }

        @Override
        protected String refineResponse(String request, String rawResponse) {
            return rawResponse;
        }

        @Override
        protected void validate(String request, String response) {
        }
    }

    private static class RecordingLlmChat extends LlmChat<String, String> {

        private final List<String> calls = new ArrayList<>();

        RecordingLlmChat(ChatClientCaller chatClientCaller) {
            super(chatClientCaller, "system", "user");
        }

        @Override
        protected String processSystemPrompt(String request) {
            calls.add("processSystem");
            return getSystemPrompt();
        }

        @Override
        protected String processUserPrompt(String request) {
            calls.add("processUser");
            return getUserPrompt();
        }

        @Override
        protected String refineResponse(String request, String rawResponse) {
            calls.add("refineResponse");
            return rawResponse;
        }

        @Override
        protected void validate(String request, String response) {
            calls.add("validate");
        }
    }
}
