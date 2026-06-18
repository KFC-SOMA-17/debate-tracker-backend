package com.debatetracker.infra.stt.config;

import com.debatetracker.infra.stt.session.azure.AzureSessionCreator;
import com.debatetracker.infra.stt.client.SttClient;
import com.debatetracker.infra.stt.router.SttClientRouter;
import com.debatetracker.infra.stt.router.SttSessionCreator;
import com.debatetracker.infra.stt.router.SttSessionRepository;
import com.debatetracker.infra.stt.repository.InMemorySttSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties({AzureConfig.class, AudioProperties.class})
public class SttAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "stt.azure.enabled", havingValue = "true")
    public SttClient sttClient(SttSessionCreator sessionCreator,
                               SttSessionRepository sessionRepository) {
        return new SttClientRouter(sessionCreator, sessionRepository);
    }

    @Bean
    @ConditionalOnProperty(name = "stt.azure.enabled", havingValue = "true")
    public SttSessionCreator azureSessionCreator(AzureConfig azureConfig,
                                                 AudioProperties audioProperties,
                                                 ApplicationEventPublisher eventPublisher) {
        log.info("[STT] AzureConfig 주입 확인 — region={}, language={}, silenceTimeoutMs={}",
                azureConfig.region(), azureConfig.language(), azureConfig.silenceTimeoutMs());
        log.info("[STT] AudioProperties 주입 확인 — sampleRate={}, channels={}, encoding={}, chunkDurationMs={}",
                audioProperties.sampleRate(),
                audioProperties.channels(),
                audioProperties.encoding(),
                audioProperties.chunkDurationMs());
        return new AzureSessionCreator(azureConfig, audioProperties, eventPublisher);
    }

    @Bean
    public SttSessionRepository sttSessionRepository() {
        return new InMemorySttSessionRepository();
    }
}
