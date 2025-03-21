package com.vut.mystrategy.configuration;

import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.service.EntryLongSignalMonitor;
import com.vut.mystrategy.service.ExitLongSignalMonitor;
import com.vut.mystrategy.service.RedisClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class DataFetcherManager {
    private final RedisClientService redisClientService;
    private final SymbolConfigManager symbolConfigManager;
    private final EntryLongSignalMonitor entryLongSignalMonitor;
    private final ExitLongSignalMonitor exitLongSignalMonitor;

    @Autowired
    public DataFetcherManager(RedisClientService redisClientService,
                              SymbolConfigManager symbolConfigManager,
                              EntryLongSignalMonitor entryLongSignalMonitor,
                              ExitLongSignalMonitor exitLongSignalMonitor) {
        this.redisClientService = redisClientService;
        this.symbolConfigManager = symbolConfigManager;
        this.entryLongSignalMonitor = entryLongSignalMonitor;
        this.exitLongSignalMonitor = exitLongSignalMonitor;
    }

    @Bean("dataFetchersMap")
    public Map<String, DataFetcher> dataFetchersMap() {
        Map<String, DataFetcher> dataFetcherMap = new ConcurrentHashMap<>();
        symbolConfigManager.getActiveSymbolConfigsList().forEach(symbolConfig -> {
            String key = KeyUtility.getDataFetcherHashMapKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol());
            DataFetcher dataFetcher = new DataFetcher(redisClientService,
                    entryLongSignalMonitor,
                    exitLongSignalMonitor,
                    symbolConfig);
            dataFetcherMap.put(key, dataFetcher);
        });
        log.info("Data fetchers map initialized: {}", dataFetcherMap);
        return dataFetcherMap;
    }
}
