package com.vut.mystrategy.service;

import com.vut.mystrategy.model.BaseOrderResponse;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.TradeSignal;
import org.springframework.stereotype.Service;

@Service
public interface AbstractOrderManager {
    //call API to place order
    BaseOrderResponse placeOrder(TradeSignal tradeSignal, SymbolConfig symbolConfig);
    //call API to close order
    BaseOrderResponse closeOrder(TradeSignal tradeSignal, SymbolConfig symbolConfig);
}
