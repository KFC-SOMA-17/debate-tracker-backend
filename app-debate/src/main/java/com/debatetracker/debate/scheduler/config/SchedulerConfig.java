package com.debatetracker.debate.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * @Scheduled 트리거를 전용 스레드 풀에서 돌린다.
 *
 * <p>기본 스케줄러는 단일 스레드라, 한 틱의 작업이 길어지면 다음 fixedRate 틱이 밀리고 여러 스케줄 작업이
 * 서로를 막는다. 보정 틱은 세션 수만큼 LLM 호출(블로킹)을 도는 무거운 작업이므로, 풀을 두어 틱 간 중첩 실행을
 * 허용하고 단일 스레드 병목을 푼다.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {

    private static final int POOL_SIZE = 4;
    private static final String THREAD_NAME_PREFIX = "refine-scheduler-";
    private static final int AWAIT_TERMINATION_SECONDS = 20;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(POOL_SIZE);
        scheduler.setThreadNamePrefix(THREAD_NAME_PREFIX);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
    }
}
