package com.debatetracker.infra.stt.config;

import com.debatetracker.infra.stt.adapter.azure.AzureAdapter;
import com.debatetracker.infra.stt.client.SttClient;
import com.debatetracker.infra.stt.repository.AzureTranscriberSessionRepository;
import com.debatetracker.infra.stt.repository.InMemoryAzureTranscriberSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties({AzureConfig.class, AudioProperties.class})
public class SttAutoConfiguration {

    @Bean
    public AzureTranscriberSessionRepository azureTranscriberSessionRepository() {
        return new InMemoryAzureTranscriberSessionRepository();
    }

    @Bean
    @ConditionalOnProperty(name = "stt.azure.enabled", havingValue = "true")
    public SttClient azureSttClient(AzureConfig azureConfig,
                                    AudioProperties audioProperties,
                                    AzureTranscriberSessionRepository sessionRepository) {
        log.info("[STT] AzureConfig 주입 확인 — region={}, language={}, silenceTimeoutMs={}",
                azureConfig.region(), azureConfig.language(), azureConfig.silenceTimeoutMs());

        log.info("[STT] AudioProperties 주입 확인 — sampleRate={}, channels={}, encoding={}, chunkDurationMs={}",
                audioProperties.sampleRate(),
                audioProperties.channels(),
                audioProperties.encoding(),
                audioProperties.chunkDurationMs()
        );
        return new AzureAdapter(azureConfig, audioProperties, sessionRepository);
    }
}
