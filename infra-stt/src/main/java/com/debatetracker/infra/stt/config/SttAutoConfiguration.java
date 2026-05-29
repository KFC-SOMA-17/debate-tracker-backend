package com.debatetracker.infra.stt.config;

import com.debatetracker.infra.stt.adapter.azure.AzureAdapter;
import com.debatetracker.infra.stt.client.SttClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AzureConfig.class, AudioProperties.class})
public class SttAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "stt.azure.enabled", havingValue = "true")
    public SttClient azureSttClient(AzureConfig azureConfig, AudioProperties audioProperties) {
        return new AzureAdapter(azureConfig, audioProperties);
    }
}
