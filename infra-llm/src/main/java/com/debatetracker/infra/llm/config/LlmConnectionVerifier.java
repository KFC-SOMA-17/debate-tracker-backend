package com.debatetracker.infra.llm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Slf4j
public class LlmConnectionVerifier {

    private static final String PING_PROMPT = "ping";
    private static final int REPLY_PREVIEW_LIMIT = 40;

    private final ChatModel chatModel;
    private final String region;
    private final String refineModel;
    private final String extractModel;

    public LlmConnectionVerifier(ChatModel chatModel, String region, String refineModel, String extractModel) {
        this.chatModel = chatModel;
        this.region = region;
        this.refineModel = refineModel;
        this.extractModel = extractModel;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verify() {
        log.info("[LLM] AWS Bedrock 연결 확인 시작 - region={}, refineModel={}, extractModel={}",
                region, refineModel, extractModel);

        long startedAt = System.nanoTime();
        try {
            String reply = ChatClient.builder(chatModel)
                    .defaultOptions(ChatOptions.builder().model(refineModel).build())
                    .build()
                    .prompt()
                    .user(PING_PROMPT)
                    .call()
                    .content();

            long elapsedMs = elapsedMillis(startedAt);
            log.info("[LLM] AWS Bedrock 연결 성공 - region={}, model={}, latency={}ms, reply='{}'",
                    region, refineModel, elapsedMs, preview(reply));
        } catch (RuntimeException exception) {
            long elapsedMs = elapsedMillis(startedAt);
            log.error("[LLM] AWS Bedrock 연결 실패 - region={}, model={}, latency={}ms. "
                            + "자격증명(AWS_ACCESS_KEY_ID/SECRET)·리전·모델 접근권한을 확인하세요.",
                    region, refineModel, elapsedMs, exception);
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String preview(String reply) {
        if (reply == null) {
            return "";
        }
        String normalized = reply.replaceAll("\\s+", " ").strip();
        if (normalized.length() <= REPLY_PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, REPLY_PREVIEW_LIMIT) + "...";
    }
}
