package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.AveragePrice;
import com.vut.mystrategy.model.EmaPrice;
import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class ExponentialMovingAverageCalculator {

    private final RedisClientService redisClientService;
    private final Integer redisTradeEventMaxSize;

    private final BigDecimal SMOOTHING_FACTOR;

    @Autowired
    public ExponentialMovingAverageCalculator(RedisClientService redisClientService,
                                              @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize,
                                              @Qualifier("emaPeriod") Integer emaPeriod) {
        this.redisClientService = redisClientService;
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;

        SMOOTHING_FACTOR =  new BigDecimal(2).divide(
                new BigDecimal(emaPeriod + 1), 4, RoundingMode.DOWN); // 2/(5+1) = 0.3333
    }

    @Async("calculateEmaPriceAsync")
    public void calculateAveragePrice(String exchangeName, String symbol) {
        String smaRedisKey = Utility.getSmaPriceRedisKey(exchangeName, symbol);
        if(!redisClientService.exists(smaRedisKey)) {
            return;
        }
        String emaRedisKey = Utility.getEmaPriceRedisKey(exchangeName, symbol);
        //Get prevEmaPrice in second time, first time get smaPrice
        BigDecimal prevEmaPrice = redisClientService.exists(emaRedisKey)
                ? redisClientService.getDataByIndex(emaRedisKey, 0, AveragePrice.class).getPrice()
                : redisClientService.getDataByIndex(smaRedisKey, 0, AveragePrice.class).getPrice();

        //Get first trade event
        String tradeEventRedisKey = Utility.getTradeEventRedisKey(exchangeName, symbol);
        BigDecimal currPrice = redisClientService.getDataByIndex(tradeEventRedisKey, 0, TradeEvent.class).getPriceAsBigDecimal();
        BigDecimal avgPrice = Calculator.calculateEmaPrice(currPrice, prevEmaPrice, SMOOTHING_FACTOR);

        //Save redis
        EmaPrice averagePrice = EmaPrice.builder()
                .exchangeName(exchangeName)
                .symbol(symbol)
                .price(avgPrice)
                .timestamp(System.currentTimeMillis())
                .build();

        redisClientService.saveDataAsList(emaRedisKey, averagePrice, redisTradeEventMaxSize - 1);
        LogMessage.printInsertRedisLogMessage(log, emaRedisKey, averagePrice);
    }
}
