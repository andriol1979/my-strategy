package com.vut.mystrategy.service.binance;

import com.vut.mystrategy.model.BaseOrderResponse;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.AbstractOrderManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.ta4j.core.Trade;

@Slf4j
@Service
@Profile("prod")
public class ProdBinanceOrderManager implements AbstractOrderManager {

    @Override
    public BaseOrderResponse placeOrder(Trade enterTrade, SymbolConfig symbolConfig, boolean isShort) {
        return null;
    }

    @Override
    public BaseOrderResponse exitOrder(Trade exitTrade, SymbolConfig symbolConfig, boolean isShort) {
        return null;
    }
}
