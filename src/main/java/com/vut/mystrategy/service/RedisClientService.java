package com.vut.mystrategy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class RedisClientService {
    /*
        All methods in this class should be called inside block code
        redisConfig.executeWithRetry(() -> {
            method here...
            ...
        }
     */

    private final ObjectMapper mapper = new ObjectMapper();

    // save trade event in redis
    @SneakyThrows
    public void saveDataAsList(@NonNull Jedis jedis, String redisKey, Object object, long maxSize) {
        String json = mapper.writeValueAsString(object);
        //save trade event in redis
        jedis.lpush(redisKey, json);
        jedis.ltrim(redisKey, 0, maxSize);
    }

    // save LotSizeResponse in redis
    @SneakyThrows
    public void saveDataAsSingle(@NonNull Jedis jedis, String redisKey, Object object) {
        String json = mapper.writeValueAsString(object);
        //save LotSizeResponse in redis
        jedis.set(redisKey, json);
    }

    public <T> List<T> getDataList(@NonNull Jedis jedis, String redisKey,
                                               long start, long stop, Class<T> clazz) {
        List<String> groupJsonList = jedis.lrange(redisKey, start, stop);
        return groupJsonList.stream()
                .map(groupJson -> {
                    try {
                        return mapper.readValue(groupJson, clazz);
                    }
                    catch (Exception e) {
                        log.error("Error deserializing {}: {}. Error: {}", clazz.getSimpleName(), groupJson, e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull).toList();
    }

    @SneakyThrows
    public <T> T getDataByKeyAndIndex(@NonNull Jedis jedis, String redisKey, int index, Class<T> clazz) {
        String redisDataJson = jedis.lindex(redisKey, index);
        return StringUtils.isBlank(redisDataJson)
                ? null
                : mapper.readValue(redisDataJson, clazz);
    }

    @SneakyThrows
    public <T> T getDataByKey(@NonNull Jedis jedis, String redisKey, Class<T> clazz) {
        String redisDataJson = jedis.get(redisKey);
        return StringUtils.isBlank(redisDataJson)
                ? null
                : mapper.readValue(redisDataJson, clazz);
    }
}
