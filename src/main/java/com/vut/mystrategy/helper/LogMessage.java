package com.vut.mystrategy.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;

public class LogMessage {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public static void printInsertRedisLogMessage(Logger log, String redisKey, Object object) {
        log.info("Inserted {} to Redis. Key: {} - Value: {} - Thread: {}",
                object.getClass().getSimpleName(), redisKey,
                objectMapper.writeValueAsString(object), Thread.currentThread().getName());
    }

    @SneakyThrows
    public static void printUpdateRedisLogMessage(Logger log, String redisKey, Object object) {
        log.info("Updated {} to Redis. Key: {} - Value: {} - Thread: {}",
                object.getClass().getSimpleName(), redisKey,
                objectMapper.writeValueAsString(object), Thread.currentThread().getName());
    }

    @SneakyThrows
    public static void printObjectLogMessage(Logger log, Object object) {
        log.info("Debugging data: Type: {} - Value: {} - Thread: {}",
                object.getClass().getSimpleName(),
                objectMapper.writeValueAsString(object), Thread.currentThread().getName());
    }
}
