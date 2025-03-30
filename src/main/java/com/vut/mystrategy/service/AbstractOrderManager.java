package com.vut.mystrategy.service;

import com.vut.mystrategy.model.BaseOrderResponse;
import com.vut.mystrategy.model.SymbolConfig;
import org.springframework.stereotype.Service;
import org.ta4j.core.Trade;

@Service
public interface AbstractOrderManager {
    //call API to place order
    BaseOrderResponse placeOrder(Trade enterTrade, SymbolConfig symbolConfig, boolean isShort);
    //call API to close order
    BaseOrderResponse exitOrder(Trade exitTrade, SymbolConfig symbolConfig, boolean isShort);
}
