package com.debatetracker.infra.stt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stt.azure")
public record AzureConfig(
        boolean enabled,
        String subscriptionKey,
        String region,
        String language,
        String profanity,
        int silenceTimeoutMs
) {
}
