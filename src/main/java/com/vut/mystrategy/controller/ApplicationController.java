package com.vut.mystrategy.controller;

import com.vut.mystrategy.configuration.feeddata.binance.BinanceWebSocketClient;
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

    @PostMapping("/disconnect")
    public ResponseEntity<String> disconnectWebSocket() {
        try {
            log.info("Received request to disconnect Binance WebSocket");
            binanceWebSocketClient.disconnect();
            return ResponseEntity.ok("WebSocket disconnected successfully");
        } catch (Exception e) {
            log.error("Error disconnecting WebSocket: {}", e.getMessage());
            return ResponseEntity.status(500).body("Failed to disconnect WebSocket: " + e.getMessage());
        }
    }
}
