package com.vut.mystrategy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RedisClientService {

    private final Jedis jedis;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public RedisClientService(Jedis jedis) {
        this.jedis = jedis;
    }

    // Lưu TradeEvent vào Redis List
    @Async("taskExecutor")
    public void saveTradeEvent(String symbol, TradeEvent tradeEvent) throws Exception {
        String key = Utility.getTradeEventRedisKey(symbol);
        String json = mapper.writeValueAsString(tradeEvent);
        jedis.lpush(key, json); // Thêm vào đầu danh sách
        jedis.ltrim(key, 0, 9); // Giữ tối đa 10 phần tử
        log.info(LogMessage.printLogMessage("Inserted tradeEvent to Redis"));
    }

    // Lấy danh sách TradeEvent
    public List<TradeEvent> getTradeEvents(String symbol) throws Exception {
        String key = Utility.getTradeEventRedisKey(symbol);
        List<String> jsonList = jedis.lrange(key, 0, -1); // Lấy tất cả
        return jsonList.stream()
                .map(json -> {
                    try {
                        return mapper.readValue(json, TradeEvent.class);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
