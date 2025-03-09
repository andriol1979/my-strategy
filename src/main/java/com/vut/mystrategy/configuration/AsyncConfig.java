package com.vut.mystrategy.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);        // Số thread tối thiểu
        executor.setMaxPoolSize(10);        // Số thread tối đa
        executor.setQueueCapacity(100);     // Hàng đợi task chờ xử lý
        executor.setThreadNamePrefix("AsyncThread-"); // Tiền tố tên thread
        executor.setWaitForTasksToCompleteOnShutdown(true); // Chờ task hoàn thành khi shutdown
        executor.setAwaitTerminationSeconds(5); // Đợi tối đa 60s
        executor.initialize();
        return executor;
    }
}
