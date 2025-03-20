package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.SymbolConfigManager;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.SumVolume;
import com.vut.mystrategy.model.TempSumVolume;
import com.vut.mystrategy.model.binance.TradeEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SumVolumeCalculator {

    private final SymbolConfigManager symbolConfigManager;
    private final RedisClientService redisClientService;
    private final VolumeTrendAnalyzer volumeTrendAnalyzer;

    private final Integer redisStorageMaxSize;

    @Autowired
    public SumVolumeCalculator(SymbolConfigManager symbolConfigManager,
                               RedisClientService redisClientService,
                               VolumeTrendAnalyzer volumeTrendAnalyzer,
                               @Qualifier("redisStorageMaxSize") Integer redisStorageMaxSize) {
        this.symbolConfigManager = symbolConfigManager;
        this.redisClientService = redisClientService;
        this.volumeTrendAnalyzer = volumeTrendAnalyzer;
        this.redisStorageMaxSize = redisStorageMaxSize;
    }

    //scheduler
    @PostConstruct
    public void init() {
        List<SymbolConfig> symbolConfigList = symbolConfigManager.getActiveSymbolConfigsList();
        symbolConfigList.forEach(symbolConfig -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() ->
                calculateSumVolume(symbolConfig), symbolConfig.getSumVolumePeriod() + 25000,
                    symbolConfig.getSumVolumePeriod(), TimeUnit.MILLISECONDS
            );
        });
    }

    public void calculateTempSumVolume(String exchangeName, String symbol, TradeEvent tradeEvent) {
        String tempSumVolumeRedisKey = KeyUtility.getTempSumVolumeRedisKey(exchangeName, symbol);
        TempSumVolume tempSumVolume = redisClientService.getDataAsSingle(tempSumVolumeRedisKey, TempSumVolume.class);
        if(tempSumVolume == null) {
            tempSumVolume = TempSumVolume.builder()
                    .exchangeName(exchangeName)
                    .symbol(symbol)
                    .bullTakerVolume(BigDecimal.ZERO)
                    .bullMakerVolume(BigDecimal.ZERO)
                    .bearTakerVolume(BigDecimal.ZERO)
                    .bearMakerVolume(BigDecimal.ZERO)
                    .build();
        }

        //sum bull/bear volume based on taker/maker
        BigDecimal quantity = new BigDecimal(tradeEvent.getQuantity());
        if (!tradeEvent.isBuyerMaker()) { // Buyer taker, Seller maker
            tempSumVolume.setBullTakerVolume(tempSumVolume.getBullTakerVolume().add(quantity));
            tempSumVolume.setBearMakerVolume(tempSumVolume.getBearMakerVolume().add(quantity));
        }
        else { // Seller taker, Buyer maker
            tempSumVolume.setBearTakerVolume(tempSumVolume.getBearTakerVolume().add(quantity));
            tempSumVolume.setBullMakerVolume(tempSumVolume.getBullMakerVolume().add(quantity));
        }
        tempSumVolume.setTimestamp(System.currentTimeMillis());

        //save to redis
        redisClientService.saveDataAsSingle(tempSumVolumeRedisKey, tempSumVolume);
        LogMessage.printInsertRedisLogMessage(log, tempSumVolumeRedisKey, tempSumVolume);
    }

    @Async("calculateSumVolumeAsync")
    public void calculateSumVolume(SymbolConfig symbolConfig) {
        String exchangeName = symbolConfig.getExchangeName();
        String symbol = symbolConfig.getSymbol();

        String tempSumVolumeRedisKey = KeyUtility.getTempSumVolumeRedisKey(exchangeName, symbol);
        // get and reset TempSumVolume in redis
        TempSumVolume tempSumVolume = redisClientService.getDataAndDeleteAsSingle(tempSumVolumeRedisKey, TempSumVolume.class);
        log.info("Reset TempSumVolume for key: {}", tempSumVolumeRedisKey);
        if(tempSumVolume == null) {
            return;
        }
        BigDecimal bullVolume = Calculator.calculateVolumeBasedOnWeight(tempSumVolume.getBullTakerVolume(),
                tempSumVolume.getBullMakerVolume(), symbolConfig.getSumVolumeTakerWeight(), symbolConfig.getSumVolumeMakerWeight());
        BigDecimal bearVolume = Calculator.calculateVolumeBasedOnWeight(tempSumVolume.getBearTakerVolume(),
                tempSumVolume.getBearMakerVolume(), symbolConfig.getSumVolumeTakerWeight(), symbolConfig.getSumVolumeMakerWeight());
        BigDecimal bullBearVolumeDivergence = Calculator.calculateBullBearVolumeDivergence(bullVolume, bearVolume);
        SumVolume sumVolume = SumVolume.builder()
                .exchangeName(exchangeName)
                .symbol(symbol)
                .bullVolume(bullVolume)
                .bearVolume(bearVolume)
                .bullBearVolumeDivergence(bullBearVolumeDivergence)
                .timestamp(System.currentTimeMillis())
                .build();

        //save redis
        String volumeRedisKey = KeyUtility.getVolumeRedisKey(exchangeName, symbol);
        redisClientService.saveDataAsList(volumeRedisKey, sumVolume, redisStorageMaxSize);
        LogMessage.printInsertRedisLogMessage(log, volumeRedisKey, sumVolume);

        //call method analyzing volume trend
        volumeTrendAnalyzer.analyzeVolumeTrend(exchangeName, symbol, symbolConfig);
    }
}
