package com.vut.mystrategy.service.binance;

import com.vut.mystrategy.model.BaseOrderResponse;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.binance.BinanceOrderResponse;
import com.vut.mystrategy.service.AbstractOrderManager;
import com.vut.mystrategy.service.OrderService;
import com.vut.mystrategy.service.RedisClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.ta4j.core.Trade;

@Slf4j
@Service
@Profile("dev")
public class DevBinanceOrderManager extends AbstractOrderManager {

    @Autowired
    public DevBinanceOrderManager(RedisClientService redisClientService,
                                  OrderService orderService) {
        super(redisClientService, orderService);
    }

    @Override
    public BaseOrderResponse placeOrder(Trade enterTrade, SymbolConfig symbolConfig, boolean isShort) {
        return new BinanceOrderResponse();
    }

    @Override
    public BaseOrderResponse exitOrder(Trade exitTrade, SymbolConfig symbolConfig, boolean isShort) {
        return null;
    }
}
