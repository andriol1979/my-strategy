package com.vut.mystrategy.controller;

import com.vut.mystrategy.helper.ApiUrlConstant;
import com.vut.mystrategy.model.StrategyRunningRequest;
import com.vut.mystrategy.service.testing.FeedDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@Slf4j
@RestController
@RequestMapping(ApiUrlConstant.TESTING_URL + "/orders")
@Validated
public class OrderTestingController {

    private final FeedDataService feedDataService;

    public OrderTestingController(FeedDataService feedDataService) {
        this.feedDataService = feedDataService;
    }

    @PostMapping("place")
    @Async
    public ResponseEntity<?> placeOrder(@RequestBody StrategyRunningRequest request) {
        feedDataService.runStrategyTesting(request);
        return AsyncResult.forValue(ResponseEntity.ok("Strategy " + request.getMyStrategyMapKey() + " is running..."));
    }
}
