package com.debatetracker.debate.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @Retryable AOP 프록시(전사 보정 재시도 등)를 활성화한다.
 */
@Configuration
@EnableRetry
public class RetryConfig {

}
