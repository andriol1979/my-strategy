package com.vut.mystrategy.configuration;

import com.vut.mystrategy.component.binance.starter.SymbolConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private final SymbolConfigManager symbolConfigManager;

    @Autowired
    public AsyncConfig(SymbolConfigManager symbolConfigManager) {
        this.symbolConfigManager = symbolConfigManager;
    }

    @Bean(name = "binanceWebSocketAsync")
    @Primary
    public Executor binanceWebSocketExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("BinanceWebSocket-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "myStrategyManagerAsync")
    public Executor myStrategyManagerExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("MStrategyManager-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "jpaTaskAsync")
    public Executor jpaTaskExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("JPATask-"); // Tiền tố tên thread
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
        int activeSymbolCount = symbolConfigManager.getActiveSymbolConfigsList().size();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(activeSymbolCount);        // Số thread tối thiểu
        executor.setMaxPoolSize(activeSymbolCount * 5);        // Số thread tối đa
        executor.setQueueCapacity(activeSymbolCount * 20);     // Hàng đợi task chờ xử lý
        executor.setWaitForTasksToCompleteOnShutdown(true); // Chờ task hoàn thành khi shutdown
        executor.setAwaitTerminationSeconds(2); // Đợi tối đa 5s
        return executor;
    }
}
