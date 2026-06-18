package com.debatetracker.infra.llm.chat;

import com.debatetracker.infra.llm.log.LlmChatLogger;
import com.debatetracker.infra.llm.log.LlmOperationType;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

@Slf4j
@RequiredArgsConstructor
public class ChatClientCaller implements LlmCaller {

    private static final String FINISH_REASON_SAFETY = "SAFETY";
    private static final String FINISH_REASON_MAX_TOKENS = "MAX_TOKENS";

    private final ChatClient chatClient;
    private final LlmChatLogger chatLogger;
    private final LlmOperationType operationType;

    @Override
    public <T> T call(String systemPrompt, String userPrompt, Function<String, T> responseProcessor) {
        ChatResponse chatResponse = chatLogger.executeWithMetrics(operationType, () -> chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .chatResponse());

        String model = recordResponseMetadata(chatResponse);
        String text = chatResponse.getResult().getOutput().getText();

        return processResponse(responseProcessor, text, model);
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
            chatLogger.recordTokenUsage(operationType, model, promptTokens, generationTokens);


            log.debug("[{}] Response ID: {}, Model: {}, Tokens: prompt={}, generation={}, total={}",
                    operationType.getValue(), responseId, model, promptTokens, generationTokens, totalTokens);
        }

        String finishReason = chatResponse.getResult().getMetadata().getFinishReason();
        if (finishReason != null && !finishReason.isBlank()) {
            chatLogger.recordFinishReason(operationType, model, finishReason);

            if (FINISH_REASON_SAFETY.equals(finishReason)) {
                log.warn("[{}] Safety filtering triggered for response: {}", operationType.getValue(), responseId);
            }
            if (FINISH_REASON_MAX_TOKENS.equals(finishReason)) {
                log.warn("[{}] Max tokens exceeded for response: {}", operationType.getValue(), responseId);
            }
        }

        return model;
    }

    private <T> T processResponse(Function<String, T> responseProcessor, String text, String model) {
        try {
            return responseProcessor.apply(text);
        } catch (RuntimeException exception) {
            chatLogger.recordValidationError(operationType, model, exception);
            throw exception;
        }
    }
}
