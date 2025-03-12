package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.AveragePrice;
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
public class AveragePriceCalculator {

    private final RedisClientService redisClientService;
    private final Integer redisTradeEventMaxSize;
    private final Integer redisTradeEventGroupSize;

    @Autowired
    public AveragePriceCalculator(RedisClientService redisClientService,
                                  @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize,
                                  @Qualifier("redisTradeEventGroupSize") Integer redisTradeEventGroupSize) {
        this.redisClientService = redisClientService;
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;
        this.redisTradeEventGroupSize = redisTradeEventGroupSize;
    }

    @Async("calculateAveragePriceAsync")
    public void calculateAveragePrice(String exchangeName, String symbol) {
        String tradeEventRedisKey = Utility.getTradeEventRedisKey(exchangeName, symbol);
        List<TradeEvent> groupTradeEventList = redisClientService.getDataList(tradeEventRedisKey,
                0, redisTradeEventGroupSize - 1, TradeEvent.class);

        BigDecimal avgPrice = Calculator.calculateTradeEventAveragePrice(groupTradeEventList, redisTradeEventGroupSize);
        if(avgPrice != null) {
            String averageKey = Utility.getTradeEventAveragePriceRedisKey(exchangeName, symbol);
            AveragePrice averagePrice = AveragePrice.builder()
                    .exchangeName(exchangeName)
                    .symbol(symbol)
                    .price(avgPrice)
                    .timestamp(System.currentTimeMillis())
                    .build();

            redisClientService.saveDataAsList(averageKey, averagePrice, redisTradeEventMaxSize - 1);
            LogMessage.printInsertRedisLogMessage(log, averageKey, averagePrice);
        }
    }
}
