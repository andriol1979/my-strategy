package com.vut.mystrategy.configuration;

import com.vut.mystrategy.configuration.binance.BinanceExchangeInfoConfig;
import com.vut.mystrategy.model.KlineIntervalEnum;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.service.RedisClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ApplicationStartupHandler {

    private final SymbolConfigManager symbolConfigManager;
    private final RedisClientService redisClientService;
    private final BinanceExchangeInfoConfig binanceExchangeInfoConfig;

    @Autowired
    public ApplicationStartupHandler(SymbolConfigManager symbolConfigManager,
                                     RedisClientService redisClientService,
                                     BinanceExchangeInfoConfig binanceExchangeInfoConfig) {
        this.symbolConfigManager = symbolConfigManager;
        this.redisClientService = redisClientService;
        this.binanceExchangeInfoConfig = binanceExchangeInfoConfig;
    }

    @EventListener
    public void onApplicationStart(ContextRefreshedEvent event) {
        //Get trading config
        List<SymbolConfig> symbolConfigList = symbolConfigManager.getActiveSymbolConfigsList();
        symbolConfigList.forEach(symbolConfig -> {
            // Delete the disposable data (need real-time data)
            List<String> redisKeys = collectAllRedisKeys(symbolConfig);
            redisKeys.forEach(redisKey -> {
                boolean result = redisClientService.deleteDataByKey(redisKey);
                log.info("Deleted Redis data by Key: {} - Status: {}", redisKey, result);
            });
        });
        //load lot size Binance
        binanceExchangeInfoConfig.loadLotSize();
        log.info("Application started");
    }

    private List<String> collectAllRedisKeys(SymbolConfig symbolConfig) {
        List<String> redisKeys = new ArrayList<>();
        for(String klineInterval : symbolConfig.getFeedKlineIntervals()) {
            KlineIntervalEnum klineEnum = KlineIntervalEnum.fromValue(klineInterval);
            redisKeys.add(KeyUtility.getKlineRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol(), klineEnum));
        }
        redisKeys.add(KeyUtility.getTradeEventIdRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol()));
        redisKeys.add(KeyUtility.getSmaCounterRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol()));
        redisKeys.add(KeyUtility.getSmaIndicatorRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol()));
        redisKeys.add(KeyUtility.getShortEmaPriceRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol()));
        redisKeys.add(KeyUtility.getLongEmaPriceRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol()));
        redisKeys.add(KeyUtility.getVolumeRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol()));
        redisKeys.add(KeyUtility.getFutureLotSizeRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol()));
        redisKeys.add(KeyUtility.getSmaTrendRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol()));
        redisKeys.add(KeyUtility.getVolumeTrendRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol()));
        redisKeys.add(KeyUtility.getTempSumVolumeRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol()));

        return redisKeys;
    }
}
