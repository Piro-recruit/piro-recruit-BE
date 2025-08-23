package com.pirogramming.recruit.domain.googleform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * GoogleForm 도메인 비동기 처리 설정
 */
@Configuration
@EnableAsync
@Slf4j
public class GoogleFormAsyncConfig {

    /**
     * GoogleForm 이벤트 처리용 스레드 풀
     */
    @Bean(name = "googleFormEventExecutor")
    public Executor googleFormEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("GoogleFormEvent-");
        
        // 예외 처리 핸들러
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("GoogleForm 이벤트 처리 작업이 거부됨: {}", r.toString());
        });
        
        executor.initialize();
        return executor;
    }
}