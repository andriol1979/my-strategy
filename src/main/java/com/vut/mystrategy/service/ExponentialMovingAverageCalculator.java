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
    private final SmaTrendAnalyzer smaTrendAnalyzer;
    private final Integer redisTradeEventMaxSize;

    private final BigDecimal SMOOTHING_FACTOR;

    @Autowired
    public ExponentialMovingAverageCalculator(RedisClientService redisClientService,
                                              SmaTrendAnalyzer smaTrendAnalyzer,
                                              @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize,
                                              @Qualifier("emaPeriod") Integer emaPeriod) {
        this.redisClientService = redisClientService;
        this.smaTrendAnalyzer = smaTrendAnalyzer;
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;
        SMOOTHING_FACTOR = Calculator.calculateEmaSmoothingFactor(emaPeriod); // 2/(5+1) = 0.3333
    }

    @Async("calculateEmaPriceAsync")
    public void calculateAveragePrice(String exchangeName, String symbol) {
        String smaRedisKey = KeyUtility.getSmaPriceRedisKey(exchangeName, symbol);
        if(!redisClientService.exists(smaRedisKey)) {
            return;
        }
        String emaRedisKey = KeyUtility.getEmaPriceRedisKey(exchangeName, symbol);
        //Get prevEmaPrice in second time, first time get smaPrice
        BigDecimal prevEmaPrice = redisClientService.exists(emaRedisKey)
                ? redisClientService.getDataByIndex(emaRedisKey, 0, AveragePrice.class).getPrice()
                : redisClientService.getDataByIndex(smaRedisKey, 0, AveragePrice.class).getPrice();

        //Get first trade event
        String tradeEventRedisKey = KeyUtility.getTradeEventRedisKey(exchangeName, symbol);
        BigDecimal currPrice = redisClientService.getDataByIndex(tradeEventRedisKey, 0, TradeEvent.class).getPriceAsBigDecimal();
        BigDecimal avgPrice = Calculator.calculateEmaPrice(currPrice, prevEmaPrice, SMOOTHING_FACTOR);

        //Save redis
        EmaPrice averagePrice = EmaPrice.builder()
                .exchangeName(exchangeName)
                .symbol(symbol)
                .price(avgPrice)
                .timestamp(System.currentTimeMillis())
                .build();

        redisClientService.saveDataAsList(emaRedisKey, averagePrice, redisTradeEventMaxSize);
        LogMessage.printInsertRedisLogMessage(log, emaRedisKey, averagePrice);
    }
}
