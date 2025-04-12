package com.vut.mystrategy.service.binance;

import com.vut.mystrategy.entity.Order;
import com.vut.mystrategy.model.*;
import com.vut.mystrategy.model.binance.BinanceOrderResponse;
import com.vut.mystrategy.service.BaseOrderBuilder;

public class BinanceOrderBuilder extends BaseOrderBuilder {

    public static Order buildOrder(BinanceOrderResponse entryResponse, BinanceOrderResponse exitResponse,
                                   SymbolConfig symbolConfig) {
        Order order = new Order();
        order.setOrderId(entryResponse.getOrderId());
        order.setClientOrderId(entryResponse.getClientOrderId());
        order.setExchangeName(entryResponse.getExchange());
        order.setSymbol(entryResponse.getSymbol());
        order.setSide(entryResponse.getSide());
        order.setPositionSide(entryResponse.getPositionSide());
        order.setEntryPrice(entryResponse.getAvgPriceAsBigDecimal());
        order.setEntryIndex(entryResponse.getBarIndex());

        order.setExitPrice(exitResponse.getAvgPriceAsBigDecimal());
        order.setExitIndex(exitResponse.getBarIndex());

        order.setExecutedQty(entryResponse.getExecutedQtyAsBigDecimal());
        order.setCumQuote(entryResponse.getCumQuoteAsBigDecimal());
        order.setLeverage(symbolConfig.getLeverage());
        order.setSlippage(symbolConfig.getSlippage());
        order.setStatus(exitResponse.getStatus());
        order.setType(entryResponse.getType());

        order.setCreatedAt(entryResponse.getTransactTime());
        order.setUpdatedAt(System.currentTimeMillis());
        order.setClosedAt(exitResponse.getTransactTime());
        order.setPnl(calculatePnL(order));

        return order;
    }
}
