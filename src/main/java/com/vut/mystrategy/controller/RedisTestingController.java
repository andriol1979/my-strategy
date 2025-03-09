package com.vut.mystrategy.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vut.mystrategy.model.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.TradeEvent;
import com.vut.mystrategy.service.RedisClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/redis-test")
public class RedisTestingController {

    private final RedisClientService redisClientService;

    @Autowired
    public RedisTestingController(RedisClientService redisClientService) {
        this.redisClientService = redisClientService;
    }

    @GetMapping("/trade-events")
    public ResponseEntity<?> getTradeEvents(@RequestParam String symbol) {
        List<TradeEvent> tradeEvents = redisClientService.getTradeEvents(symbol);
        log.info("Received tradeEvents from Redis: {}", tradeEvents);
        return ResponseEntity.ok(tradeEvents);
    }

    @GetMapping("/lot-sizes")
    public ResponseEntity<?> getLotSize(@RequestParam String symbol) throws JsonProcessingException {
        BinanceFutureLotSizeResponse lotSizeResponse = redisClientService.getFutureLotSizeFilter(symbol);
        log.info("Received lotSizeResponse from Redis: {}", lotSizeResponse);
        return ResponseEntity.ok(lotSizeResponse);
    }
}
