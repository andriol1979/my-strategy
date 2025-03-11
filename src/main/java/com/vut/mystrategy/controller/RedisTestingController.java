package com.vut.mystrategy.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vut.mystrategy.helper.ApiUrlConstant;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.TradeEvent;
import com.vut.mystrategy.service.TradeEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping(ApiUrlConstant.TESTING_URL + "/redis")
public class RedisTestingController {

    private final TradeEventService tradeEventService;

    @Autowired
    public RedisTestingController(TradeEventService tradeEventService) {
        this.tradeEventService = tradeEventService;
    }

    @GetMapping("/trade-events")
    public ResponseEntity<?> getTradeEvents(@RequestParam String exchangeName, @RequestParam String symbol) {
        List<TradeEvent> tradeEvents = tradeEventService.getTradeEvents(exchangeName, symbol);
        log.info("Received tradeEvents from Redis: {}", tradeEvents);
        return ResponseEntity.ok(tradeEvents);
    }

    @GetMapping("/lot-sizes")
    public ResponseEntity<?> getLotSize(@RequestParam String exchangeName, @RequestParam String symbol) throws JsonProcessingException {
        if(exchangeName.equalsIgnoreCase(Constant.EXCHANGE_NAME_BINANCE)) {
            Optional<BinanceFutureLotSizeResponse> optional = tradeEventService.getBinanceFutureLotSizeFilter(symbol);
            log.info("Received lotSizeResponse from Redis: {}", optional.orElse(null));
            return ResponseEntity.ok(optional.orElse(null));
        }

        return ResponseEntity.noContent().build();
    }
}
