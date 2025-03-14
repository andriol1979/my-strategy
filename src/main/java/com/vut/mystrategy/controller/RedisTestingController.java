package com.vut.mystrategy.controller;

import com.vut.mystrategy.helper.ApiUrlConstant;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.AveragePrice;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiUrlConstant.TESTING_URL + "/redis")
public class RedisTestingController {

    private final RedisClientService redisClientService;

    @Autowired
    public RedisTestingController(RedisClientService redisClientService) {
        this.redisClientService = redisClientService;
    }

    @GetMapping("/trade-events")
    public ResponseEntity<?> getTradeEvents(@RequestParam String exchangeName, @RequestParam String symbol) {
        String tradeEventRedisKey = KeyUtility.getTradeEventRedisKey(exchangeName, symbol);
        List<TradeEvent> tradeEvents = redisClientService.getDataList(tradeEventRedisKey, 0, -1, TradeEvent.class);
        log.info("Received tradeEvents from Redis: {}", tradeEvents);
        return ResponseEntity.ok(tradeEvents);
    }

    @GetMapping("/lot-sizes")
    public ResponseEntity<?> getLotSize(@RequestParam String exchangeName, @RequestParam String symbol) {
        if(exchangeName.equalsIgnoreCase(Constant.EXCHANGE_NAME_BINANCE)) {
            String redisKey = KeyUtility.getFutureLotSizeRedisKey(exchangeName, symbol);
            BinanceFutureLotSizeResponse lotSizeResponse = redisClientService.getDataAsSingle(redisKey, BinanceFutureLotSizeResponse.class);
            return ResponseEntity.ok(lotSizeResponse);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/average-prices")
    public ResponseEntity<?> getAveragePrices(@RequestParam String symbol) {
        String averageKey = KeyUtility.getSmaPriceRedisKey(Constant.EXCHANGE_NAME_BINANCE, symbol);
        // Dùng executeWithRetry để lấy danh sách JSON từ Redis
        List<AveragePrice> averageList = redisClientService.getDataList(averageKey, 0, 1, AveragePrice.class);
        return ResponseEntity.ok(averageList);
    }

    @GetMapping("/average-price")
    public ResponseEntity<?> getAveragePrice(@RequestParam String symbol,
                                              @RequestParam int index) {
        String averageKey = KeyUtility.getSmaPriceRedisKey(Constant.EXCHANGE_NAME_BINANCE, symbol);
        AveragePrice averagePrice = redisClientService.getDataByIndex(averageKey, index, AveragePrice.class);
        return ResponseEntity.ok(averagePrice);
    }
}
