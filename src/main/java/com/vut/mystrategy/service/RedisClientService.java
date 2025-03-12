package com.vut.mystrategy.service;

import com.vut.mystrategy.entity.TradingConfig;
import com.vut.mystrategy.helper.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RedisClientService {
    private final RedisTemplate<String, Object> redisTemplate;

    private final RedisTemplate<String, String> counterTemplate;

    @Autowired
    public RedisClientService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.counterTemplate = new RedisTemplate<>();
        this.counterTemplate.setConnectionFactory(redisTemplate.getConnectionFactory());
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        this.counterTemplate.setKeySerializer(stringSerializer);
        this.counterTemplate.setValueSerializer(stringSerializer);
        this.counterTemplate.afterPropertiesSet();
    }

    public void saveDataAsList(String redisKey, Object object, long maxSize) {
        try {
            redisTemplate.opsForList().leftPush(redisKey, object); // Lưu object trực tiếp
            redisTemplate.opsForList().trim(redisKey, 0, maxSize - 1); // Cắt list
        } catch (Exception e) {
            log.error("Error saving data to Redis key {}: {}", redisKey, e.getMessage(), e);
            throw e;
        }
    }

    public void saveDataAsSingle(String redisKey, Object object) {
        try {
            redisTemplate.opsForValue().set(redisKey, object);
        } catch (Exception e) {
            log.error("Error saving data to Redis key {} as single value: {}", redisKey, e.getMessage(), e);
            throw e;
        }
    }

    public <T> T getDataByIndex(String redisKey, int index, Class<T> clazz) {
        try {
            Object data = redisTemplate.opsForList().index(redisKey, index);
            if (data == null) {
                log.warn("No data found at key {} index {}", redisKey, index);
                return null;
            }
            log.debug("Fetched data from key {} at index {}: {}", redisKey, index, data);
            return clazz.cast(data); // Ép kiểu về T
        } catch (Exception e) {
            log.error("Error fetching data from key {} at index {}: {}", redisKey, index, e.getMessage(), e);
            throw e;
        }
    }

    public <T> List<T> getDataList(String redisKey, long start, long stop, Class<T> clazz) {
        try {
            List<Object> dataList = redisTemplate.opsForList().range(redisKey, start, stop);
            if (dataList == null || dataList.isEmpty()) {
                log.warn("No data found for key {}", redisKey);
                return Collections.emptyList();
            }
            log.debug("Fetched list from key {}: size={}", redisKey, dataList.size());
            return dataList.stream()
                    .map(clazz::cast)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting list from Redis key {}: {}", redisKey, e.getMessage(), e);
            throw e;
        }
    }

    public <T> T getDataAsSingle(String redisKey, Class<T> clazz) {
        try {
            Object data = redisTemplate.opsForValue().get(redisKey);
            if (data == null) {
                log.warn("No data found for key {}", redisKey);
                return null;
            }
            log.debug("Fetched single value from key {}: {}", redisKey, data);
            return clazz.cast(data);
        } catch (Exception e) {
            log.error("Error fetching single value from key {}: {}", redisKey, e.getMessage(), e);
            throw e;
        }
    }

    public boolean exists(String redisKey) {
        try {
            return redisTemplate.hasKey(redisKey);
        } catch (Exception e) {
            log.error("Error checking existence of key {}: {}", redisKey, e.getMessage(), e);
            return false;
        }
    }

    public Long incrementCounter(String counterKey) {
        try {
            return counterTemplate.opsForValue().increment(counterKey);
        } catch (Exception e) {
            log.error("Error incrementing counter {}: {}", counterKey, e.getMessage(), e);
            return null;
        }
    }

    public void resetCounter(String counterKey) {
        try {
            counterTemplate.opsForValue().set(counterKey, "0");
            log.debug("Reset counter {} to 0", counterKey);
        } catch (Exception e) {
            log.error("Error resetting counter {}: {}", counterKey, e.getMessage(), e);
        }
    }

    public void resetCounter(List<TradingConfig> tradingConfigs) {
        tradingConfigs.forEach(tradingConfig -> {
            String counterKey = Utility.getSmaCounterRedisKey(tradingConfig.getExchangeName(), tradingConfig.getSymbol());
            resetCounter(counterKey);
        });
    }
}
