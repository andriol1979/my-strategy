package com.vut.mystrategy.controller;

import com.vut.mystrategy.configuration.binance.BinanceWebSocketClient;
import com.vut.mystrategy.helper.ApiUrlConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(ApiUrlConstant.REAL_URL + "/app")
@Validated
public class ApplicationController {

    private final BinanceWebSocketClient binanceWebSocketClient;

    public ApplicationController(BinanceWebSocketClient binanceWebSocketClient) {
        this.binanceWebSocketClient = binanceWebSocketClient;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startApplication() {
        binanceWebSocketClient.connectToBinance();
        return ResponseEntity.ok("Application started");
    }
}
