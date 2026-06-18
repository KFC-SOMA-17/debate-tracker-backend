package com.debatetracker.infra.llm.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.debatetracker.infra.llm.log.LlmChatLogger;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;

class ChatClientCallerTest {

    @Nested
    class Call {

        @Test
        void 성공_시_요청_지표와_응답_지표를_기록하고_콜백_결과를_반환한다() {
            // given
            ChatClient chatClient = mockChatClient("LLM response text", "gemini-2.5-flash", "STOP", null);
            LlmChatLogger logger = mock(LlmChatLogger.class, Answers.RETURNS_DEEP_STUBS);
            ChatClientCaller caller = new ChatClientCaller(chatClient, logger, "refine");

            // when
            String result = caller.call("system", "user", Function.identity());

            // then
            assertAll(
                    () -> assertThat(result).isEqualTo("LLM response text"),
                    () -> verify(logger).startRequestTimer(),
                    () -> verify(logger).recordRequestSuccess(eq("refine")),
                    () -> verify(logger).recordFinishReason(eq("refine"), eq("gemini-2.5-flash"), eq("STOP")),
                    () -> verify(logger, never()).recordRequestError(anyString(), any())
            );
        }

        @Test
        void 실패_시_에러_지표를_기록하고_예외를_던진다() {
            // given
            ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
            RuntimeException apiError = new RuntimeException("API error");
            when(chatClient.prompt()
                    .system(anyString())
                    .user(anyString())
                    .call()
                    .chatResponse())
                    .thenThrow(apiError);

            LlmChatLogger logger = mock(LlmChatLogger.class, Answers.RETURNS_DEEP_STUBS);
            ChatClientCaller caller = new ChatClientCaller(chatClient, logger, "extract");

            // when & then
            assertAll(
                    () -> assertThatThrownBy(() -> caller.call("system", "user", Function.identity()))
                            .isInstanceOf(RuntimeException.class)
                            .hasMessage("API error"),
                    () -> verify(logger).startRequestTimer(),
                    () -> verify(logger).recordRequestError(eq("extract"), eq(apiError)),
                    () -> verify(logger, never()).recordRequestSuccess(anyString())
            );
        }

        @Test
        void 토큰_사용량이_있으면_토큰_지표를_기록한다() {
            // given
            ChatClient chatClient = mockChatClientWithTokens("response", "gemini-2.5-flash", 100, 50);
            LlmChatLogger logger = mock(LlmChatLogger.class, Answers.RETURNS_DEEP_STUBS);
            ChatClientCaller caller = new ChatClientCaller(chatClient, logger, "refine");

            // when
            caller.call("system", "user", Function.identity());

            // then
            verify(logger).recordTokenUsage(eq("refine"), eq("gemini-2.5-flash"), eq(100), eq(50));
        }

        @Test
        void 콜백에서_예외_발생_시_validation_에러_지표를_기록한다() {
            // given
            ChatClient chatClient = mockChatClient("response", "gemini-2.5-pro", null, null);
            LlmChatLogger logger = mock(LlmChatLogger.class, Answers.RETURNS_DEEP_STUBS);
            ChatClientCaller caller = new ChatClientCaller(chatClient, logger, "extract");

            // when & then
            assertAll(
                    () -> assertThatThrownBy(() -> caller.call("system", "user", text -> new RuntimeException("Validation failed")))
                            .isInstanceOf(RuntimeException.class)
                            .hasMessage("Validation failed"),
                    () -> verify(logger).recordValidationError(eq("extract"), eq("gemini-2.5-pro"), eq(new RuntimeException("Validation failed")))
            );
        }
    }

    private ChatClient mockChatClient(String text, String model, String finishReason, Integer promptTokens) {
        ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .chatResponse()
                .getResult().getOutput().getText())
                .thenReturn(text);
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .chatResponse()
                .getMetadata().getModel())
                .thenReturn(model);
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .chatResponse()
                .getMetadata().getId())
                .thenReturn("resp-123");
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .chatResponse()
                .getMetadata().getUsage())
                .thenReturn(null);
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .chatResponse()
                .getResult().getMetadata().getFinishReason())
                .thenReturn(finishReason);
        return chatClient;
    }

    private ChatClient mockChatClientWithTokens(String text, String model, int prompt, int completion) {
        ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .chatResponse()
                .getResult().getOutput().getText())
                .thenReturn(text);
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .chatResponse()
                .getMetadata().getModel())
                .thenReturn(model);
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .chatResponse()
                .getMetadata().getId())
                .thenReturn("resp-123");
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .chatResponse()
                .getMetadata().getUsage().getPromptTokens())
                .thenReturn(prompt);
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .chatResponse()
                .getMetadata().getUsage().getCompletionTokens())
                .thenReturn(completion);
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .chatResponse()
                .getMetadata().getUsage().getTotalTokens())
                .thenReturn(prompt + completion);
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .chatResponse()
                .getResult().getMetadata().getFinishReason())
                .thenReturn(null);
        return chatClient;
    }
}
