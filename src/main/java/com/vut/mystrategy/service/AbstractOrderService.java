package com.vut.mystrategy.service;

import com.vut.mystrategy.entity.Order;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.model.SymbolConfig;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ta4j.core.TradingRecord;

import java.math.BigDecimal;

@Slf4j
@Service
@NoArgsConstructor
public abstract class AbstractOrderService {

    @Async("jpaTaskAsync")
    public abstract void saveOrderToDb(Order order);
    public abstract Order buildOrder(TradingRecord tradingRecord, SymbolConfig symbolConfig, boolean isShort);

    protected BigDecimal calculatePnL(Order order) {
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
