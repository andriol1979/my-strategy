package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;

@Slf4j
@Service
public class PriceTrendingMonitor {

    private final RedisClientService redisClientService;
    private final Integer redisTradeEventMaxSize;
    private final Integer redisTradeEventGroupSize;

    @Autowired
    public PriceTrendingMonitor(
            RedisClientService redisClientService,
            @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize,
            @Qualifier("redisTradeEventGroupSize") Integer redisTradeEventGroupSize) {
        this.redisClientService = redisClientService;
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;
        this.redisTradeEventGroupSize = redisTradeEventGroupSize;
    }

    @Async("priceTrendingMonitorAsync")
    public void calculateAveragePrice(Jedis jedis, String exchangeName, String symbol) {
        String tradeEventRedisKey = Utility.getTradeEventRedisKey(exchangeName, symbol);
        List<TradeEvent> groupTradeEventList = redisClientService.getDataList(jedis, tradeEventRedisKey,
                0, redisTradeEventGroupSize - 1, TradeEvent.class);

        String average = Calculator.calculateTradeEventAveragePrice(groupTradeEventList, redisTradeEventGroupSize);
        if(StringUtils.isNotBlank(average)) {
            String averageKey = Utility.getTradeEventAveragePriceRedisKey(exchangeName, symbol);
            redisClientService.saveDataAsList(jedis, averageKey, average, redisTradeEventMaxSize - 1);
            log.info(LogMessage.printLogMessage("Inserted Average price to Redis. Symbol: {} - Average price: {}"), symbol, average);
        }
    }
}
