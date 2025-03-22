package com.vut.mystrategy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CounterPeriodService {

    private final RedisClientService redisClientService;

    @Autowired
    public CounterPeriodService(RedisClientService redisClientService) {
        this.redisClientService = redisClientService;
    }

    public boolean checkCounterPeriod(String counterKey, int period) {
        //Increase counter and get new value
        Long counter = redisClientService.incrementCounter(counterKey);
        if (counter == null) counter = 0L;
        if(counter < period || counter % period != 0) {
            return false;
        }
        // reset counter
        redisClientService.resetCounter(counterKey);
        return true;
    }
}
