package com.debatetracker.debate.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 트리거(전사 보정 틱 등)를 활성화한다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

}
