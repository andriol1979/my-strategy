package com.vut.mystrategy.configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DataFetcherScheduler {

    private final Map<String, DataFetcher> dataFetchersMap;
    @Value("${analyze-scheduler-initial-delay}")
    private long analyzeSchedulerInitialDelay;

    @Autowired
    public DataFetcherScheduler(@Qualifier("dataFetchersMap") Map<String, DataFetcher> dataFetchersMap) {
        this.dataFetchersMap = dataFetchersMap;
        log.info("DataFetcherScheduler created. Size = {}", dataFetchersMap.size());
    }
    //trigger fetchMarketData from DataFetcher instance
    @PostConstruct
    public void init() {
        dataFetchersMap.keySet().forEach(key -> {
            log.info("DataFetcherScheduler is initializing for key {}", key);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            DataFetcher dataFetcher = dataFetchersMap.get(key);
            scheduler.scheduleAtFixedRate(dataFetcher::fetchMarketData,
                    analyzeSchedulerInitialDelay - 50, //chạy trước trading signal 50 milli
                    dataFetcher.getSymbolConfig().getFetchDataDelayTime(), TimeUnit.MILLISECONDS
            );
        });
    }
}
