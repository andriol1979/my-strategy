package com.vut.mystrategy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.configuration.RedisEmbeddedConfig;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RedisClientService {

    private final RedisEmbeddedConfig redisConfig;
    private final PriceTrendingMonitor priceTrendingMonitor;
    private final Integer redisTradeEventMaxSize;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public RedisClientService(RedisEmbeddedConfig redisConfig,
                              PriceTrendingMonitor priceTrendingMonitor,
                              @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize) {
        this.redisConfig = redisConfig;
        this.priceTrendingMonitor = priceTrendingMonitor;
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;
    }

    // Lưu TradeEvent vào Redis List
    @Async("binanceWebSocketAsync")
    public void saveTradeEvent(String exchangeName, String symbol, TradeEvent tradeEvent) throws JsonProcessingException {
        String tradeEventRedisKey = Utility.getTradeEventRedisKey(exchangeName, symbol);
        String json = mapper.writeValueAsString(tradeEvent);

        // Gọi Redis qua executeWithRetry
        redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            //save trade event in redis
            jedis.lpush(tradeEventRedisKey, json);
            jedis.ltrim(tradeEventRedisKey, 0, redisTradeEventMaxSize - 1);
            log.info(LogMessage.printLogMessage("Inserted TradeEvent to Redis. Symbol={}, Price={}"),
                    tradeEvent.getSymbol(), tradeEvent.getPrice());

            //After save trade event success -> calculate average price and store redis
            priceTrendingMonitor.calculateAveragePrice(jedis, exchangeName, symbol);
            return null;
        });
    }

    // Get TradeEvent list
    public List<TradeEvent> getTradeEvents(String exchangeName, String symbol) {
        String key = Utility.getTradeEventRedisKey(exchangeName, symbol);

        // Dùng executeWithRetry để lấy danh sách JSON từ Redis
        List<String> jsonList = redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            return jedis.lrange(key, 0, -1);
        });

        // Parse JSON thành TradeEvent
        if (jsonList == null || jsonList.isEmpty()) {
            log.info("No trade events found for {}", symbol);
            return Collections.emptyList();
        }

        return jsonList.stream()
                .map(json -> {
                    try {
                        return mapper.readValue(json, TradeEvent.class);
                    } catch (Exception e) {
                        log.error("Error deserializing tradeEvent: {}", json, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    //Get first trade event
    public Optional<TradeEvent> getNewestTradeEvent(String exchangeName, String symbol) throws JsonProcessingException {
        String key = Utility.getTradeEventRedisKey(exchangeName, symbol);

        // Dùng executeWithRetry để lấy JSON từ Redis
        String json = redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            return jedis.lindex(key, 0);
        });
        if (json != null) {
            return Optional.of(mapper.readValue(json, TradeEvent.class));
        }
        return Optional.empty();
    }

    public void saveFutureLotSize(String exchangeName, String symbol, BinanceFutureLotSizeResponse futureLotSize) throws JsonProcessingException {
        String key = Utility.getFutureLotSizeRedisKey(exchangeName, symbol);
        String json = mapper.writeValueAsString(futureLotSize);

        // Gọi Redis qua executeWithRetry
        redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            jedis.set(key, json);
            log.info("Stored LOT_SIZE for {}: {}", symbol, json);
            return null; // Không cần trả về gì
        });
    }

    public Optional<BinanceFutureLotSizeResponse> getBinanceFutureLotSizeFilter(String symbol) throws JsonProcessingException {
        String key = Utility.getFutureLotSizeRedisKey(Constant.EXCHANGE_NAME_BINANCE, symbol);
        String json = redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            return jedis.get(key);
        });
        if (json != null) {
            return Optional.of(mapper.readValue(json, BinanceFutureLotSizeResponse.class));
        }
        return Optional.empty();
    }
}
