package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.AveragePrice;
import com.vut.mystrategy.model.EmaPrice;
import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class ExponentialMovingAverageCalculator {

    private final RedisClientService redisClientService;
    private final Integer redisTradeEventMaxSize;

    private final BigDecimal SHORT_SMOOTHING_FACTOR;
    private final BigDecimal LONG_SMOOTHING_FACTOR;

    @Autowired
    public ExponentialMovingAverageCalculator(RedisClientService redisClientService,
                                              @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize,
                                              @Qualifier("emaShortPeriod") Integer emaShortPeriod,
                                              @Qualifier("emaLongPeriod") Integer emaLongPeriod) {
        this.redisClientService = redisClientService;
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;
        SHORT_SMOOTHING_FACTOR = Calculator.calculateEmaSmoothingFactor(emaShortPeriod); // 2/(5+1) = 0.3333
        LONG_SMOOTHING_FACTOR = Calculator.calculateEmaSmoothingFactor(emaLongPeriod); // 2/(10+1) = 0.1819
    }

    @Async("calculateShortEmaPriceAsync")
    public void calculateShortEmaPrice(String exchangeName, String symbol, TradeEvent currTradeEvent) {
        String smaRedisKey = KeyUtility.getSmaPriceRedisKey(exchangeName, symbol);
        if(!redisClientService.exists(smaRedisKey)) {
            return;
        }
        //Get prevShortEMAPrice in second time, first time get SMAPrice
        String shortEmaRedisKey = KeyUtility.getShortEmaPriceRedisKey(exchangeName, symbol);
        BigDecimal prevShortEmaPrice = redisClientService.exists(shortEmaRedisKey)
                ? redisClientService.getDataByIndex(shortEmaRedisKey, 0, AveragePrice.class).getPrice()
                : redisClientService.getDataByIndex(smaRedisKey, 0, AveragePrice.class).getPrice();

        BigDecimal currPrice = currTradeEvent.getPriceAsBigDecimal();
        calculateEmaPriceAndSaveRedis(exchangeName, symbol,
                currPrice, prevShortEmaPrice, SHORT_SMOOTHING_FACTOR, shortEmaRedisKey);

        //Calculate LONG EMA price based on trade event but waiting SHORT EMA is saved
        calculateLongEmaPrice(exchangeName, symbol, currPrice);
    }

    @Async("calculateLongEmaPriceAsync")
    public void calculateLongEmaPrice(String exchangeName, String symbol, BigDecimal currPrice) {
        String shortEmaRedisKey = KeyUtility.getShortEmaPriceRedisKey(exchangeName, symbol);
        if(!redisClientService.exists(shortEmaRedisKey)) {
            return;
        }
        //Get prevLongEMAPrice in second time, first time get shortEMAPrice
        String longEmaRedisKey = KeyUtility.getLongEmaPriceRedisKey(exchangeName, symbol);
        BigDecimal prevLongEmaPrice = redisClientService.exists(longEmaRedisKey)
                ? redisClientService.getDataByIndex(longEmaRedisKey, 0, AveragePrice.class).getPrice()
                : redisClientService.getDataByIndex(shortEmaRedisKey, 0, AveragePrice.class).getPrice();

        calculateEmaPriceAndSaveRedis(exchangeName, symbol,
                currPrice, prevLongEmaPrice, LONG_SMOOTHING_FACTOR, longEmaRedisKey);
    }

    private void calculateEmaPriceAndSaveRedis(String exchangeName, String symbol,
                                               BigDecimal currPrice, BigDecimal prevEmaPrice,
                                               BigDecimal smoothingFactor, String emaRedisKey) {
        // Calculate EMA based on current price and previous EMA price and smoothing factor
        BigDecimal avgPrice = Calculator.calculateEmaPrice(currPrice, prevEmaPrice, smoothingFactor);

        //Save redis
        EmaPrice emaPrice = EmaPrice.builder()
                .exchangeName(exchangeName)
                .symbol(symbol)
                .price(avgPrice)
                .timestamp(System.currentTimeMillis())
                .build();

        redisClientService.saveDataAsList(emaRedisKey, emaPrice, redisTradeEventMaxSize);
        LogMessage.printInsertRedisLogMessage(log, emaRedisKey, emaPrice);
    }
}
