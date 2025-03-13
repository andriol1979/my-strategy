package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.SmaPrice;
import com.vut.mystrategy.model.SumVolume;
import com.vut.mystrategy.model.TempSumVolume;
import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class SumVolumeCalculator {

    private final RedisClientService redisClientService;
    private final Integer redisTradeEventMaxSize;
    private final Integer sumVolumePeriod;
    private final Double sumVolumeTakerWeight;
    private final Double sumVolumeMakerWeight;

    @Autowired
    public SumVolumeCalculator(RedisClientService redisClientService,
                               @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize,
                               @Qualifier("sumVolumePeriod") Integer sumVolumePeriod,
                               @Qualifier("sumVolumeTakerWeight") Double sumVolumeTakerWeight,
                               @Qualifier("sumVolumeMakerWeight") Double sumVolumeMakerWeight) {
        this.redisClientService = redisClientService;
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;
        this.sumVolumePeriod = sumVolumePeriod;
        this.sumVolumeTakerWeight = sumVolumeTakerWeight;
        this.sumVolumeMakerWeight = sumVolumeMakerWeight;
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

        //save to redis
        redisClientService.saveDataAsSingle(tempSumVolumeRedisKey, tempSumVolume);
        LogMessage.printInsertRedisLogMessage(log, tempSumVolumeRedisKey, tempSumVolume);
    }

    @Async("calculateSumVolumeAsync")
    public void calculateSumVolume(String exchangeName, String symbol) {

    }
}
