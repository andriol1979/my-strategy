package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.SymbolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SymbolConfigManager {

    private final RedisClientService redisClientService;
    private final Integer redisTradeEventMaxSize;

    @Autowired
    public SymbolConfigManager(RedisClientService redisClientService,
                               @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize) {
        this.redisClientService = redisClientService;
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;
    }

    public SymbolConfig saveNewSymbolConfig(SymbolConfig symbolConfig) {
        //save redis
        String symbolConfigRedisKey = KeyUtility.getSymbolConfigRedisKey();
        redisClientService.saveDataAsList(symbolConfigRedisKey, symbolConfig, redisTradeEventMaxSize);
        return symbolConfig;
    }

    public List<SymbolConfig> gettAllSymbolConfigsList() {
        String symbolConfigRedisKey = KeyUtility.getSymbolConfigRedisKey();
        return redisClientService.getDataList(symbolConfigRedisKey, 0, -1, SymbolConfig.class);
    }

    public List<SymbolConfig> getActiveSymbolConfigsList() {
        String symbolConfigRedisKey = KeyUtility.getSymbolConfigRedisKey();
        List<SymbolConfig> symbolConfigsList = redisClientService.getDataList(symbolConfigRedisKey, 0, -1, SymbolConfig.class);

        return symbolConfigsList.stream()
                .filter(SymbolConfig::isActive)
                .collect(Collectors.toList());
    }

    public List<SymbolConfig> getActiveSymbolConfigsListByExchangeName(String exchangeName) {
        List<SymbolConfig> symbolConfigsList = gettAllSymbolConfigsList();

        return symbolConfigsList.stream()
                .filter(config -> config.isActive() &&
                        config.getExchangeName().equalsIgnoreCase(exchangeName))
                .collect(Collectors.toList());
    }

    public SymbolConfig getSymbolConfig(String exchangeName, String symbol) {
        List<SymbolConfig> symbolConfigsList = getActiveSymbolConfigsList();

        return findSymbolConfigFromList(symbolConfigsList, exchangeName, symbol);
    }

    public SymbolConfig updateSymbolConfig(SymbolConfig target) {
        String symbolConfigRedisKey = KeyUtility.getSymbolConfigRedisKey();
        List<SymbolConfig> symbolConfigsList = gettAllSymbolConfigsList();
        //delete from redis
        SymbolConfig symbolConfig = findSymbolConfigFromList(symbolConfigsList,
                target.getExchangeName(), target.getSymbol());
        boolean deleted = redisClientService.deleteDataExactly(symbolConfigRedisKey, symbolConfig);
        if (deleted) {
            //add new symbol config based on target
            SymbolConfig updatedSymbolConfig = new SymbolConfig(target);
            redisClientService.saveDataAsList(symbolConfigRedisKey, symbolConfig, redisTradeEventMaxSize);
            return updatedSymbolConfig;
        }
        //can not delete
        return symbolConfig;
    }

    private SymbolConfig findSymbolConfigFromList(List<SymbolConfig> symbolConfigsList,
                                                  String exchangeName, String symbol) {
        return symbolConfigsList.stream()
                .filter(config -> config.getSymbol() != null &&
                        config.getSymbol().equalsIgnoreCase(symbol) &&
                        config.getExchangeName().equalsIgnoreCase(exchangeName))
                .findFirst()
                .orElse(null);
    }
}
