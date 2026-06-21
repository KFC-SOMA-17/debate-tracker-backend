package com.debatetracker.infra.stt.config;

import com.debatetracker.infra.stt.client.SttClient;
import com.debatetracker.infra.stt.logger.SttLogger;
import com.debatetracker.infra.stt.repository.InMemorySttSessionRepository;
import com.debatetracker.infra.stt.router.SttClientRouter;
import com.debatetracker.infra.stt.router.SttSessionCreator;
import com.debatetracker.infra.stt.router.SttSessionRepository;
import com.debatetracker.infra.stt.session.azure.AzureSessionCreator;
import io.micrometer.core.instrument.MeterRegistry;
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
    public SttLogger sttLogger(MeterRegistry meterRegistry, SttSessionRepository sessionRepository) {
        return new SttLogger(meterRegistry, sessionRepository);
    }

    @Bean
    @ConditionalOnProperty(name = "stt.azure.enabled", havingValue = "true")
    public SttClient sttClient(SttSessionCreator sessionCreator,
                               SttSessionRepository sessionRepository,
                               SttLogger sttLogger) {
        return new SttClientRouter(sessionCreator, sessionRepository, sttLogger);
    }

    @Bean
    @ConditionalOnProperty(name = "stt.azure.enabled", havingValue = "true")
    public SttSessionCreator azureSessionCreator(AzureConfig azureConfig,
                                                 AudioProperties audioProperties,
                                                 ApplicationEventPublisher eventPublisher,
                                                 SttLogger sttLogger) {
        log.info("[STT] AzureConfig 주입 확인 — region={}, language={}, silenceTimeoutMs={}",
                azureConfig.region(), azureConfig.language(), azureConfig.silenceTimeoutMs());
        log.info("[STT] AudioProperties 주입 확인 — sampleRate={}, channels={}, encoding={}, chunkDurationMs={}",
                audioProperties.sampleRate(),
                audioProperties.channels(),
                audioProperties.encoding(),
                audioProperties.chunkDurationMs());
        return new AzureSessionCreator(azureConfig, audioProperties, eventPublisher, sttLogger);
    }

    @Bean
    public SttSessionRepository sttSessionRepository() {
        return new InMemorySttSessionRepository();
    }
}
