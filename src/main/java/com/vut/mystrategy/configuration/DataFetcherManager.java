package com.vut.mystrategy.configuration;

import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.service.RedisClientService;
import com.vut.mystrategy.service.SymbolConfigManager;
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

    @Autowired
    public DataFetcherManager(RedisClientService redisClientService,
                              SymbolConfigManager symbolConfigManager) {
        this.redisClientService = redisClientService;
        this.symbolConfigManager = symbolConfigManager;
    }

    @Bean("dataFetchersMap")
    public Map<String, DataFetcher> dataFetchersMap() {
        Map<String, DataFetcher> dataFetcherMap = new ConcurrentHashMap<>();
        symbolConfigManager.getActiveSymbolConfigsList().forEach(symbolConfig -> {
            String key = KeyUtility.getDataFetcherHashMapKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol());
            DataFetcher dataFetcher = new DataFetcher(redisClientService, symbolConfig);
            dataFetcherMap.put(key, dataFetcher);
        });
        log.info("Data fetchers map initialized: {}", dataFetcherMap);
        return dataFetcherMap;
    }
}
