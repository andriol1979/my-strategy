package com.vut.mystrategy.configuration;

import com.vut.mystrategy.entity.TradingConfig;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.service.RedisClientService;
import com.vut.mystrategy.service.TradingConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ApplicationStartupHandler {

    private final TradingConfigManager tradingConfigManager;
    private final RedisClientService redisClientService;

    @Autowired
    public ApplicationStartupHandler(TradingConfigManager tradingConfigManager,
                                     RedisClientService redisClientService) {
        this.tradingConfigManager = tradingConfigManager;
        this.redisClientService = redisClientService;
    }

    @EventListener
    public void onApplicationStart(ContextRefreshedEvent event) {
        //Get trading config
        List<TradingConfig> tradingConfigList = tradingConfigManager.getAllActiveConfigs();
        tradingConfigList.forEach(tradingConfig -> {
            //SmaTrend
            String smaTrendRedisKey = KeyUtility.getSmaTrendRedisKey(tradingConfig.getExchangeName(), tradingConfig.getSymbol());
            String volumeTrendRedisKey = KeyUtility.getVolumeTrendRedisKey(tradingConfig.getExchangeName(), tradingConfig.getSymbol());
            boolean smaDeleted = redisClientService.deleteDataByKey(smaTrendRedisKey);
            boolean volumeDeleted = redisClientService.deleteDataByKey(volumeTrendRedisKey);
            log.info("Deleted SMA Trend data by Key: {} - Status: {}", smaTrendRedisKey, smaDeleted);
            log.info("Deleted Volume Trend data by Key {} - Status: {}", volumeTrendRedisKey, volumeDeleted);
        });
        log.info("Application started");
    }
}
