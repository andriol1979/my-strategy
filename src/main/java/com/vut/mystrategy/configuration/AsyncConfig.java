package com.vut.mystrategy.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "binanceWebSocketAsync")
    public Executor binanceWebSocketExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("BinanceWebSocket-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "calculateSmaPriceAsync")
    public Executor calculateSmaPriceExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("SMAPrice-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "calculateEmaPriceAsync")
    public Executor calculateEmaPriceExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("EMAPrice-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "priceTrendingMonitorAsync")
    public Executor priceTrendingMonitorExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("PriceTrendingMonitor-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("AsyncUncaughtExceptionHandler: ", ex);
            log.error("Error in method async task {}: {}", method.getName(), ex.getMessage());
        };
    }

    private ThreadPoolTaskExecutor buildThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);        // Số thread tối thiểu
        executor.setMaxPoolSize(10);        // Số thread tối đa
        executor.setQueueCapacity(100);     // Hàng đợi task chờ xử lý
        executor.setWaitForTasksToCompleteOnShutdown(true); // Chờ task hoàn thành khi shutdown
        executor.setAwaitTerminationSeconds(5); // Đợi tối đa 5s
        return executor;
    }
}
