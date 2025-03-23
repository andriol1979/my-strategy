package com.vut.mystrategy.controller;

import com.vut.mystrategy.helper.ApiUrlConstant;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.service.RedisClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(ApiUrlConstant.TESTING_URL + "/redis")
public class RedisTestingController {

    private final RedisClientService redisClientService;

    @Autowired
    public RedisTestingController(RedisClientService redisClientService) {
        this.redisClientService = redisClientService;
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
}
