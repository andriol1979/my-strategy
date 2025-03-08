package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TradeEventDataProcessor {
    private final RedisClientService redisClientService;

    @Autowired
    public TradeEventDataProcessor(RedisClientService redisService) {
        this.redisClientService = redisService;
    }

    @Async("taskExecutor")
    public void processData(TradeEvent tradeEvent) {
        try {
            log.info(LogMessage.printLogMessage("Processing tradeEvent"));
            redisClientService.saveTradeEvent(tradeEvent.getSymbol(), tradeEvent);
        }
        catch (Exception e) {
            log.error("Exception when processing TradeEvent: {}", e.getMessage());
        }
    }
}
