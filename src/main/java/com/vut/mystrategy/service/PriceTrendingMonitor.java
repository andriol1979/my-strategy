package com.vut.mystrategy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Objects;

@Slf4j
@Service
public class PriceTrendingMonitor {

    private final Integer redisTradeEventMaxSize;
    private final Integer redisTradeEventGroupSize;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public PriceTrendingMonitor(
            @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize,
            @Qualifier("redisTradeEventGroupSize") Integer redisTradeEventGroupSize) {
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;
        this.redisTradeEventGroupSize = redisTradeEventGroupSize;
    }

    @Async("priceTrendingMonitorAsync")
    public void calculateAveragePrice(Jedis jedis, String exchangeName, String symbol) {
        String tradeEventRedisKey = Utility.getTradeEventRedisKey(exchangeName, symbol);
        List<String> groupJsonList = jedis.lrange(tradeEventRedisKey, 0, redisTradeEventGroupSize - 1);
        List<TradeEvent> groupTradeEventList = groupJsonList.stream()
                .map(groupJson -> {
                    try {
                        return mapper.readValue(groupJson, TradeEvent.class);
                    }
                    catch (Exception e) {
                        log.error("Error deserializing tradeEvent: {}. Error: {}", groupJson, e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull).toList();
        String average = Calculator.calculateTradeEventAveragePrice(groupTradeEventList, redisTradeEventGroupSize);
        if(StringUtils.isNotBlank(average)) {
            String averageKey = Utility.getTradeEventAveragePriceRedisKey(exchangeName, symbol);
            jedis.lpush(averageKey, average);
            jedis.ltrim(averageKey, 0, redisTradeEventMaxSize - 1);
            log.info(LogMessage.printLogMessage("Inserted Average price to Redis. Key: {} - Value: {}"), averageKey, average);
        }
    }
}
