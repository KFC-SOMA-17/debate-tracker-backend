package com.debatetracker.infra.llm.chat;

import com.debatetracker.infra.llm.log.LlmChatLogger;
import io.micrometer.core.instrument.Timer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

@Slf4j
@RequiredArgsConstructor
public class ChatClientCaller implements LlmCaller {

    private final ChatClient chatClient;
    private final LlmChatLogger chatLogger;
    private final String operation;

    @Override
    public <T> T call(String systemPrompt, String userPrompt, Function<String, T> responseProcessor) {
        ChatResponse chatResponse = executeWithMetrics(() -> chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .chatResponse());

        String model = recordResponseMetadata(chatResponse);
        String text = chatResponse.getResult().getOutput().getText();

        return processResponse(responseProcessor, text, model);
    }

    private <T> T executeWithMetrics(Supplier<T> action) {
        Timer.Sample sample = chatLogger.startRequestTimer();
        try {
            T result = action.get();
            chatLogger.recordRequestSuccess(operation);
            return result;
        } catch (RuntimeException exception) {
            chatLogger.recordRequestError(operation, exception);
            throw exception;
        } finally {
            chatLogger.stopRequestTimer(operation, sample);
        }
    }

    private String recordResponseMetadata(ChatResponse chatResponse) {
        ChatResponseMetadata metadata = chatResponse.getMetadata();
        String responseId = metadata.getId();
        String model = metadata.getModel();

        Usage usage = metadata.getUsage();
        if (usage != null) {
            Integer promptTokens = usage.getPromptTokens();
            Integer generationTokens = usage.getCompletionTokens();
            Integer totalTokens = usage.getTotalTokens();

            chatLogger.recordTokenUsage(operation, model, promptTokens, generationTokens);

            log.debug("[{}] Response ID: {}, Model: {}, Tokens: prompt={}, generation={}, total={}",
                    operation, responseId, model, promptTokens, generationTokens, totalTokens);
        }

        String finishReason = chatResponse.getResult().getMetadata().getFinishReason();
        if (finishReason != null && !finishReason.isBlank()) {
            chatLogger.recordFinishReason(operation, model, finishReason);

            if ("SAFETY".equals(finishReason)) {
                log.warn("[{}] Safety filtering triggered for response: {}", operation, responseId);
            }
            if ("MAX_TOKENS".equals(finishReason)) {
                log.warn("[{}] Max tokens exceeded for response: {}", operation, responseId);
            }
        }

        return model;
    }

    private <T> T processResponse(Function<String, T> responseProcessor, String text, String model) {
        try {
            return responseProcessor.apply(text);
        } catch (RuntimeException exception) {
            chatLogger.recordValidationError(operation, model, exception);
            throw exception;
        }
    }
}
