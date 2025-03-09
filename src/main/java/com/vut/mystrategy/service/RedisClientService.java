package com.vut.mystrategy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.configuration.RedisEmbeddedConfig;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RedisClientService {

    private final RedisEmbeddedConfig redisConfig;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public RedisClientService(RedisEmbeddedConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    // Lưu TradeEvent vào Redis List
    @Async("taskExecutor")
    public void saveTradeEvent(String symbol, TradeEvent tradeEvent) throws JsonProcessingException {
        String key = Utility.getTradeEventRedisKey(symbol);
        String json = mapper.writeValueAsString(tradeEvent);

        // Gọi Redis qua executeWithRetry
        redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            jedis.lpush(key, json);
            jedis.ltrim(key, 0, 9);
            log.info(LogMessage.printLogMessage("Inserted tradeEvent to Redis. Key: {}"), key);
            return null; // Không cần trả về gì
        });
    }

    // Lấy danh sách TradeEvent
    public List<TradeEvent> getTradeEvents(String symbol) {
        String key = Utility.getTradeEventRedisKey(symbol);

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

    public void saveFutureLotSize(String symbol, BinanceFutureLotSizeResponse futureLotSize) throws JsonProcessingException {
        String key = Utility.getFutureLotSizeRedisKey(symbol);
        String json = mapper.writeValueAsString(futureLotSize);

        // Gọi Redis qua executeWithRetry
        redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            jedis.set(key, json);
            log.info("Stored LOT_SIZE for {}: {}", symbol, json);
            return null; // Không cần trả về gì
        });
    }

    public BinanceFutureLotSizeResponse getFutureLotSizeFilter(String symbol) throws JsonProcessingException {
        String key = Utility.getFutureLotSizeRedisKey(symbol);
        String json = redisConfig.executeWithRetry(() -> {
            Jedis jedis = redisConfig.getJedis();
            return jedis.get(key);
        });
        if (json != null) {
            return mapper.readValue(json, BinanceFutureLotSizeResponse.class);
        }
        return null;
    }
}
