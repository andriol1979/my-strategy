package com.vut.mystrategy.service.binance;

import com.vut.mystrategy.model.BaseOrderResponse;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.AbstractOrderManager;
import com.vut.mystrategy.service.KlineEventService;
import com.vut.mystrategy.service.RedisClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.ta4j.core.Trade;

@Slf4j
@Service
@Profile("prod")
public class ProdBinanceOrderManager implements AbstractOrderManager {
    private final BinanceOrderService binanceOrderService;
    private final RedisClientService redisClientService;
    private final KlineEventService klineEventService;

    @Autowired
    public ProdBinanceOrderManager(BinanceOrderService binanceOrderService,
                                   RedisClientService redisClientService,
                                   KlineEventService klineEventService) {
        this.binanceOrderService = binanceOrderService;
        this.redisClientService = redisClientService;
        this.klineEventService = klineEventService;
    }

    @Override
    public BaseOrderResponse placeOrder(Trade enterTrade, SymbolConfig symbolConfig, boolean isShort) {
        return null;
    }

    @Override
    public BaseOrderResponse exitOrder(Trade exitTrade, SymbolConfig symbolConfig, boolean isShort) {
        return null;
    }
}
