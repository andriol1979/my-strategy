package com.vut.mystrategy.configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DataFetcherScheduler {

    private final Map<String, DataFetcher> dataFetchersMap;

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
            scheduler.scheduleAtFixedRate(dataFetcher::fetchMarketData, 60*1000,
                    700, TimeUnit.MILLISECONDS
            );
        });
    }
}
