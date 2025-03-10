package com.vut.mystrategy.controller;

import com.vut.mystrategy.helper.ApiUrlConstant;
import com.vut.mystrategy.model.MyStrategyOrderRequest;
import com.vut.mystrategy.service.MyStrategyOrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(ApiUrlConstant.REAL_URL + "/orders")
@Validated
public class MyStrategyOrderController {

    private final MyStrategyOrderService myStrategyOrderService;

    @Autowired
    public MyStrategyOrderController(MyStrategyOrderService myStrategyOrderService) {
        this.myStrategyOrderService = myStrategyOrderService;
    }

    @PostMapping()
    public ResponseEntity<?> addWaitOrder(@Valid @RequestBody MyStrategyOrderRequest request) {
        return ResponseEntity.ok(myStrategyOrderService.addWaitOrder(request));
    }
}
