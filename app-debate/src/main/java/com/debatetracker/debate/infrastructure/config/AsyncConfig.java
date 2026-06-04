package com.debatetracker.debate.infrastructure.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String EVENT_LISTENER_EXECUTOR = "eventListenerExecutor";
    public static final String CACHE_EXECUTOR = "cacheExecutor";

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 16;
    private static final int QUEUE_CAPACITY = 500;
    private static final int KEEP_ALIVE_SECONDS = 60;

    private static final int CACHE_CORE_POOL_SIZE = 2;
    private static final int CACHE_MAX_POOL_SIZE = 4;
    private static final int CACHE_QUEUE_CAPACITY = 200;

    @Bean(name = EVENT_LISTENER_EXECUTOR)
    public Executor eventListenerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix("event-listener-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 캐시 워밍(메타 캐시 재적재) 전용 풀. 지연에 민감한 STT 이벤트 경로({@link #EVENT_LISTENER_EXECUTOR})와
     * 자원을 분리해, 느린 Redis 가 broadcast 스레드를 굶기지 않도록 한다.
     */
    @Bean(name = CACHE_EXECUTOR)
    public Executor cacheExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CACHE_CORE_POOL_SIZE);
        executor.setMaxPoolSize(CACHE_MAX_POOL_SIZE);
        executor.setQueueCapacity(CACHE_QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix("cache-warm-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
