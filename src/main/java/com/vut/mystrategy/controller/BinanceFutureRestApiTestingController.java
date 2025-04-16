package com.vut.mystrategy.controller;

import com.vut.mystrategy.component.binance.BinanceFutureRestApiClient;
import com.vut.mystrategy.helper.ApiUrlConstant;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.binance.BinanceOrderRequest;
import com.vut.mystrategy.model.binance.BinanceOrderResponse;
import com.vut.mystrategy.model.binance.ListenKeyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(ApiUrlConstant.TESTING_URL + "/binance/rest-api-client")
@Validated
public class BinanceFutureRestApiTestingController {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    private final BinanceFutureRestApiClient binanceFutureRestApiClient;

    public BinanceFutureRestApiTestingController(BinanceFutureRestApiClient binanceFutureRestApiClient) {
        this.binanceFutureRestApiClient = binanceFutureRestApiClient;
    }

    @PostMapping("order")
    public ResponseEntity<?> placeOrder(@RequestBody BinanceOrderRequest orderRequest) {
        if(Utility.isProdProfile(activeProfile)) {
            throw new UnsupportedOperationException("Prod profile is not supported for testing");
        }
        BinanceOrderResponse orderResponse = binanceFutureRestApiClient.placeOrder(orderRequest);
        if(orderResponse != null) {
            return ResponseEntity.ok(orderResponse);
        }
        return ResponseEntity.internalServerError().build();
    }

    @GetMapping("listen-key")
    public ResponseEntity<?> getListenKey() {
        if(Utility.isProdProfile(activeProfile)) {
            throw new UnsupportedOperationException("Prod profile is not supported for testing");
        }
        ListenKeyResponse response = binanceFutureRestApiClient.receiveListenKey();
        if(response != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.internalServerError().build();
    }
}
