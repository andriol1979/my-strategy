package com.vut.mystrategy.service;

import com.vut.mystrategy.entity.TradingConfig;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
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

    private final TradingConfigManager tradingConfigManager;
    private final RedisClientService redisClientService;
    private final Integer redisTradeEventMaxSize;
    private final Integer sumVolumePeriod;
    private final Double sumVolumeTakerWeight;
    private final Double sumVolumeMakerWeight;

    @Autowired
    public SumVolumeCalculator(TradingConfigManager tradingConfigManager,
                               RedisClientService redisClientService,
                               @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize,
                               @Qualifier("sumVolumePeriod") Integer sumVolumePeriod,
                               @Qualifier("sumVolumeTakerWeight") Double sumVolumeTakerWeight,
                               @Qualifier("sumVolumeMakerWeight") Double sumVolumeMakerWeight) {
        this.tradingConfigManager = tradingConfigManager;
        this.redisClientService = redisClientService;
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;
        this.sumVolumePeriod = sumVolumePeriod;
        this.sumVolumeTakerWeight = sumVolumeTakerWeight;
        this.sumVolumeMakerWeight = sumVolumeMakerWeight;
    }

    //scheduler
    @PostConstruct
    public void init() {
        List<TradingConfig> tradingConfigList = tradingConfigManager.getAllActiveConfigs();
        tradingConfigList.forEach(tradingConfig -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() ->
                    calculateSumVolume(tradingConfig.getExchangeName(), tradingConfig.getSymbol()),
                    sumVolumePeriod + 2500, sumVolumePeriod, TimeUnit.MILLISECONDS
            );
        });
    }

    public void calculateTempSumVolume(String exchangeName, String symbol, TradeEvent tradeEvent) {
        String tempSumVolumeRedisKey = Utility.getTempSumVolumeRedisKey(exchangeName, symbol);
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
    public void calculateSumVolume(String exchangeName, String symbol) {
        String tempSumVolumeRedisKey = Utility.getTempSumVolumeRedisKey(exchangeName, symbol);
        // get and reset TempSumVolume in redis
        TempSumVolume tempSumVolume = redisClientService.getDataAndDeleteAsSingle(tempSumVolumeRedisKey, TempSumVolume.class);
        log.info("Reset TempSumVolume for key: {}", tempSumVolumeRedisKey);
        if(tempSumVolume == null) {
            return;
        }
        BigDecimal bullVolume = Calculator.calculateVolumeBasedOnWeight(tempSumVolume.getBullTakerVolume(),
                tempSumVolume.getBullMakerVolume(), sumVolumeTakerWeight, sumVolumeMakerWeight);
        BigDecimal bearVolume = Calculator.calculateVolumeBasedOnWeight(tempSumVolume.getBearTakerVolume(),
                tempSumVolume.getBearMakerVolume(), sumVolumeTakerWeight, sumVolumeMakerWeight);
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
        String volumeRedisKey = Utility.getVolumeRedisKey(exchangeName, symbol);
        redisClientService.saveDataAsList(volumeRedisKey, sumVolume, redisTradeEventMaxSize);
        LogMessage.printInsertRedisLogMessage(log, volumeRedisKey, sumVolume);
    }
}
