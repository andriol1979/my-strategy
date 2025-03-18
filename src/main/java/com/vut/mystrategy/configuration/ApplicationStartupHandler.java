package com.vut.mystrategy.configuration;

import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.service.RedisClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ApplicationStartupHandler {

    private final SymbolConfigManager symbolConfigManager;
    private final RedisClientService redisClientService;

    @Autowired
    public ApplicationStartupHandler(SymbolConfigManager symbolConfigManager,
                                     RedisClientService redisClientService) {
        this.symbolConfigManager = symbolConfigManager;
        this.redisClientService = redisClientService;
    }

    @EventListener
    public void onApplicationStart(ContextRefreshedEvent event) {
        //Get trading config
        List<SymbolConfig> symbolConfigList = symbolConfigManager.getActiveSymbolConfigsList();
        symbolConfigList.forEach(symbolConfig -> {
            // Delete the disposable data (need real-time data)
            String smaCounterRedisKey = KeyUtility.getTradeEventRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol());
            String tempSumVolumeRedisKey = KeyUtility.getTempSumVolumeRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol());
            String smaTrendRedisKey = KeyUtility.getSmaTrendRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol());
            String volumeTrendRedisKey = KeyUtility.getVolumeTrendRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol());
            boolean smaDeleted = redisClientService.deleteDataByKey(smaTrendRedisKey);
            boolean smaCounterDeleted = redisClientService.deleteDataByKey(smaCounterRedisKey);
            boolean tempSumVolumeDeleted = redisClientService.deleteDataByKey(tempSumVolumeRedisKey);
            boolean volumeDeleted = redisClientService.deleteDataByKey(volumeTrendRedisKey);
            log.info("Deleted SMA Trend data by Key: {} - Status: {}", smaTrendRedisKey, smaDeleted);
            log.info("Deleted SMA Counter data by Key: {} - Status: {}", smaCounterRedisKey, smaCounterDeleted);
            log.info("Deleted Volume Trend data by Key {} - Status: {}", volumeTrendRedisKey, volumeDeleted);
            log.info("Deleted Temp Sum Volume data by Key {} - Status: {}", tempSumVolumeRedisKey, tempSumVolumeDeleted);
        });
        log.info("Application started");
    }
}
