package com.vut.mystrategy.controller;

import com.vut.mystrategy.helper.ApiUrlConstant;
import com.vut.mystrategy.model.StrategyRunningRequest;
import com.vut.mystrategy.service.testing.FeedDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(ApiUrlConstant.TESTING_URL + "/strategies")
@Validated
public class StrategyTestingController {

    private final FeedDataService feedDataService;

    public StrategyTestingController(FeedDataService feedDataService) {
        this.feedDataService = feedDataService;
    }

    @PostMapping()
    public ResponseEntity<?> testStrategy(@RequestBody StrategyRunningRequest request) {
        long startedAt = System.currentTimeMillis();
        log.info("Received backtest request: strategy={}, exchange={}, symbol={}, interval={}, useNewTable={}, maxBars={}, sleepMillis={}",
                request.getMyStrategyMapKey(), request.getExchangeName(), request.getSymbol(),
                request.getKlineInterval(), request.isBackTestKlineData(), request.getMaxBars(), request.getSleepMillis());
        if(request.isBackTestKlineData()){
            feedDataService.runStrategyTestingNew(request);
        }
        else {
            feedDataService.runStrategyTesting(request);
        }
        long elapsed = System.currentTimeMillis() - startedAt;
        log.info("Finished backtest request: strategy={}, exchange={}, symbol={}, interval={}, elapsedMs={}",
                request.getMyStrategyMapKey(), request.getExchangeName(), request.getSymbol(),
                request.getKlineInterval(), elapsed);
        return ResponseEntity.ok("Strategy " + request.getMyStrategyMapKey() + " finished in " + elapsed + " ms");
    }
}
