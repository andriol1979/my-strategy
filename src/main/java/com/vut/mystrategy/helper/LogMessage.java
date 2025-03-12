package com.vut.mystrategy.helper;

import org.slf4j.Logger;

public class LogMessage {
    public static void printInsertRedisLogMessage(Logger log, String redisKey, Object object) {
        log.info("Inserted {} to Redis. Key: {} - Value: {}",
                object.getClass().getSimpleName(), redisKey, object);
    }
}
