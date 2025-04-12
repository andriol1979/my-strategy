package com.vut.mystrategy.service.binance;

import com.vut.mystrategy.model.BaseOrderResponse;
import com.vut.mystrategy.model.MyStrategyBaseBar;
import com.vut.mystrategy.model.SymbolConfig;
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
@Profile("prod")
public class ProdBinanceOrderManager extends AbstractOrderManager {

    @Autowired
    public ProdBinanceOrderManager(RedisClientService redisClientService,
                                   OrderService orderService) {
        super(redisClientService, orderService);
    }

    @Override
    public BaseOrderResponse placeOrder(MyStrategyBaseBar entryBar, int entryIndex,
                                        SymbolConfig symbolConfig, boolean isShort) {
        return null;
    }

    @Override
    public BaseOrderResponse exitOrder(BaseOrderResponse entryResponse, MyStrategyBaseBar exitBar,
                                       int exitIndex, SymbolConfig symbolConfig, boolean isShort) {
        return null;
    }
}
