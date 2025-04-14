package com.vut.mystrategy.service.order;

import com.vut.mystrategy.entity.Order;
import com.vut.mystrategy.helper.Calculator;

import java.math.BigDecimal;

public class BaseOrderBuilder {

    protected static BigDecimal calculatePnL(Order order) {
        if (order.getExitPrice() == null || order.getEntryPrice() == null) {
            return BigDecimal.ZERO;
        }
        return "BUY".equals(order.getSide())
                ? Calculator.calculateLongPnL(order.getEntryPrice(), order.getExitPrice(), order.getSlippage(),
                order.getCumQuote(), order.getLeverage())
                : Calculator.calculateShortPnL(order.getEntryPrice(), order.getExitPrice(), order.getSlippage(),
                order.getCumQuote(), order.getLeverage());
    }
}
