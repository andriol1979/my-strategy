package com.vut.mystrategy.service;

import com.vut.mystrategy.entity.Order;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.*;
import com.vut.mystrategy.model.binance.BinanceOrderResponse;
import com.vut.mystrategy.repository.OrderRepository;
import com.vut.mystrategy.service.binance.BinanceOrderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderService {
    private final OrderRepository orderRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void buildAndSaveOrder(OrderResponseStorage orderResponseStorage, SymbolConfig symbolConfig) {
        if(!validateOrderResponseStorage(orderResponseStorage)) {
            return;
        }
        Order order = null;
        if(symbolConfig.getExchangeName().equals(Constant.EXCHANGE_NAME_BINANCE)) {
            order = BinanceOrderBuilder.buildOrder((BinanceOrderResponse) orderResponseStorage.getEntryResponse(),
                    (BinanceOrderResponse) orderResponseStorage.getExitResponse(), symbolConfig);
        }
        //TODO: more exchange here
        //.....

        // save order to postgres
        if(order != null) {
            orderRepository.save(order);
            log.info("OrderService created order: {}", order);
        }
        log.warn("Cannot save order because order is null");
    }

    private boolean validateOrderResponseStorage(OrderResponseStorage orderResponseStorage) {
        if(orderResponseStorage == null) {
            log.error("OrderResponseStorage is null");
            return false;
        }
        if(orderResponseStorage.getEntryResponse() == null) {
            log.error("OrderResponseStorage.EntryResponse is null");
            return false;
        }
        if(orderResponseStorage.getExitResponse() == null) {
            log.error("OrderResponseStorage.ExitResponse is null");
            return false;
        }
        return true;
    }
}
