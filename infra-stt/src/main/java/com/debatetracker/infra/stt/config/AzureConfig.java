package com.debatetracker.infra.stt.config;

public record AzureConfig(
        boolean enabled,
        String subscriptionKey,
        String region,
        String language,
        String profanity,
        int silenceTimeoutMs
) {}
