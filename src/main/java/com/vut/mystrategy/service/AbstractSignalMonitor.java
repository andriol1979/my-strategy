package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.DataFetcher;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public abstract class AbstractSignalMonitor {

    protected final TradingSignalAnalyzer tradingSignalAnalyzer;
    protected final RedisClientService redisClientService;
    protected final Map<String, DataFetcher> dataFetchersMap;

    @Value("${analyze-scheduler-initial-delay}")
    private long analyzeSchedulerInitialDelay;

    @Autowired
    public AbstractSignalMonitor(TradingSignalAnalyzer tradingSignalAnalyzer,
                                 RedisClientService redisClientService,
                                 @Qualifier("dataFetchersMap") Map<String, DataFetcher> dataFetchersMap) {
        this.tradingSignalAnalyzer = tradingSignalAnalyzer;
        this.redisClientService = redisClientService;
        this.dataFetchersMap = dataFetchersMap;
    }

    @PostConstruct
    public void init() {
        dataFetchersMap.keySet().forEach(key -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            DataFetcher dataFetcher = dataFetchersMap.get(key);
            log.info("{} is initiated by dataFetcher {}", this.getClass().getSimpleName(), dataFetcher);
            scheduler.scheduleAtFixedRate(() ->
                    monitorSignal(dataFetcher), analyzeSchedulerInitialDelay,
                    dataFetcher.getSymbolConfig().getFetchDataDelayTime(), TimeUnit.MILLISECONDS);
        });
    }

    public abstract void monitorSignal(DataFetcher dataFetcher);
}
