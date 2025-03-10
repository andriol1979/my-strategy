package com.vut.mystrategy.configuration;

import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.repository.MyStrategyOrderRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MyStrategyOrderManager {

    private final MyStrategyOrderRepository myStrategyOrderRepository;

    @Autowired
    public MyStrategyOrderManager(MyStrategyOrderRepository myStrategyOrderRepository) {
        this.myStrategyOrderRepository = myStrategyOrderRepository;
    }

    @PostConstruct
    public void init() {
//        TO_DO
        //sync orders from binance first

        //load wait orders into global map
        myStrategyOrderRepository.findByOrderStatus(Constant.ORDER_STATUS_WAIT)
                .forEach(order ->
                        ApplicationData.MY_STRATEGY_WAIT_ORDER_MAP.put(order.getOrderId(), order));
        log.info("MyStrategyOrderManager init finished. Loaded total {} wait orders",
                ApplicationData.MY_STRATEGY_WAIT_ORDER_MAP.size());
    }
}
