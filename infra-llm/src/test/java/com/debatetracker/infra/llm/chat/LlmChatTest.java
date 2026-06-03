package com.debatetracker.infra.llm.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.client.ChatClient;

class LlmChatTest {

    @CsvSource(delimiter = '|', value = {
            "hello world                       | hello world  | 펜스 없으면 그대로(trim)",
            "'```json\n{\"a\":1}\n```'         | {\"a\":1}     | json 코드펜스 제거",
            "'```\n{\"a\":1}\n```'             | {\"a\":1}     | 언어 없는 코드펜스 제거",
            "'```json\n{\"a\":1}'              | {\"a\":1}     | 닫는 펜스 없으면 여는 펜스만 제거",
            "'```json'                         | '```json'    | 개행 없는 펜스는 원본 유지",
    })
    @ParameterizedTest(name = "[{index}] {2}")
    void stripCodeFence_handlesVariants(String raw, String expected, String description) {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        given(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .willReturn(raw);
        EchoLlmChat chat = new EchoLlmChat(() -> chatClient);

        String result = chat.fetch("ignored");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void fetch_invokesTemplateStepsInOrder() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        given(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .willReturn("raw");
        RecordingLlmChat chat = new RecordingLlmChat(() -> chatClient);

        chat.fetch("req");

        assertThat(chat.calls)
                .containsExactly("processSystem", "processUser", "refineResponse", "validate");
    }

    @Test
    void fetch_propagatesValidationFailure() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        given(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .willReturn("raw");
        EchoLlmChat chat = new EchoLlmChat(() -> chatClient) {
            @Override
            protected void validate(String request, String response) {
                throw new IllegalStateException("invalid");
            }
        };

        assertThatThrownBy(() -> chat.fetch("req"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("invalid");
    }


    private static class EchoLlmChat extends LlmChat<String, String> {

        EchoLlmChat(LlmSelector selector) {
            super(selector, "system", "user");
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

        RecordingLlmChat(LlmSelector selector) {
            super(selector, "system", "user");
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
