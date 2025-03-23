package com.vut.mystrategy.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public Executor binanceWebSocketExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("BinanceWebSocket-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "calculateSmaIndicatorAsync")
    public Executor calculateSmaIndicatorExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("SMAIndicator-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "calculateShortEmaIndicatorAsync")
    public Executor calculateShortEmaIndicatorExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ShortEMAIndicator-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "calculateLongEmaIndicatorAsync")
    public Executor calculateLongEmaIndicatorExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("LongEMAIndicator-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "calculateSumVolumeAsync")
    public Executor calculateSumVolumeExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("SumVolume-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "analyzeSmaTrendAsync")
    public Executor analyzeSmaTrendExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("SMATrend-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "analyzeVolumeTrendAsync")
    public Executor analyzeVolumeTrendExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("VolumeTrend-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "monitorEntryLongSignalAsync")
    public Executor monitorEntryLongSignalExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("EntryLongSignal-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "monitorExitLongSignalAsync")
    public Executor monitorExitLongSignalExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ExitLongSignal-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "monitorEntryShortSignalAsync")
    public Executor monitorEntryShortSignalExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("EntryShortSignal-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "monitorExitShortSignalAsync")
    public Executor monitorExitShortSignalExecutor() {
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ExitShortSignal-"); // Tiền tố tên thread
        executor.initialize();
        return executor;
    }

    @Bean(name = "marketDataFetcherAsync")
    public Executor marketDataFetcherExecutor() {
        int activeSymbolCount = symbolConfigManager.getActiveSymbolConfigsList().size();
        ThreadPoolTaskExecutor executor = buildThreadPoolTaskExecutor();
        executor.setCorePoolSize(activeSymbolCount);        // Số thread tối thiểu
        executor.setMaxPoolSize(activeSymbolCount * 2);        // Số thread tối đa
        executor.setQueueCapacity(activeSymbolCount * 5);
        executor.setThreadNamePrefix("DataFetcher-"); // Tiền tố tên thread
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
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);        // Số thread tối thiểu
        executor.setMaxPoolSize(10);        // Số thread tối đa
        executor.setQueueCapacity(100);     // Hàng đợi task chờ xử lý
        executor.setWaitForTasksToCompleteOnShutdown(true); // Chờ task hoàn thành khi shutdown
        executor.setAwaitTerminationSeconds(2); // Đợi tối đa 5s
        return executor;
    }
}
