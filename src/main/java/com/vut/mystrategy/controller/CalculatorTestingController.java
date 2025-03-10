package com.vut.mystrategy.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vut.mystrategy.helper.ApiUrlConstant;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.TradeEvent;
import com.vut.mystrategy.service.RedisClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping(ApiUrlConstant.TESTING_URL + "/calculator")
public class CalculatorTestingController {

    private final RedisClientService redisClientService;

    @Autowired
    public CalculatorTestingController(RedisClientService redisClientService) {
        this.redisClientService = redisClientService;
    }

    @GetMapping("/quantity")
    public ResponseEntity<?> calculateQuantity(@RequestParam String exchangeName, @RequestParam String symbol,
                                               @RequestParam BigDecimal amount) throws JsonProcessingException {
        Optional<TradeEvent> tradeEvent = redisClientService.getNewestTradeEvent(exchangeName, symbol);
        if(tradeEvent.isPresent()) {
            Optional<BinanceFutureLotSizeResponse> optional = redisClientService.getBinanceFutureLotSizeFilter(symbol);
            String quantity = Calculator.calculateQuantity(optional.orElseThrow(),
                    amount, tradeEvent.get().getPriceAsBigDecimal());
            return ResponseEntity.ok(quantity);
        }
        return ResponseEntity.notFound().build();
    }
}
