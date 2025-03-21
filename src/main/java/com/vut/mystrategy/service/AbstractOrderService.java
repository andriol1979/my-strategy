package com.vut.mystrategy.service;

import com.vut.mystrategy.entity.Order;
import com.vut.mystrategy.model.BaseOrderResponse;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.binance.BinanceOrderResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@NoArgsConstructor
public abstract class AbstractOrderService {

    @Async("jpaTaskAsync")
    public abstract void saveOrderToDb(Order order);
    public abstract Order buildNewOrder(BaseOrderResponse response, SymbolConfig symbolConfig);
    public abstract Order buildUpdatedOrder(Order orgOrder, BaseOrderResponse response);

    protected BigDecimal calculatePnL(Order order) {
        if (order.getExitPrice() == null || order.getEntryPrice() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal priceDiff = order.getExitPrice().subtract(order.getEntryPrice());
        return "BUY".equals(order.getSide()) ? priceDiff.multiply(order.getExecutedQty())
                : priceDiff.negate().multiply(order.getExecutedQty());
    }
}
