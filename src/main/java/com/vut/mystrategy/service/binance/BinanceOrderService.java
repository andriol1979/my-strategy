package com.vut.mystrategy.service.binance;

import com.vut.mystrategy.entity.Order;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.BaseOrderResponse;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.binance.BinanceOrderResponse;
import com.vut.mystrategy.repository.OrderRepository;
import com.vut.mystrategy.service.AbstractOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class BinanceOrderService extends AbstractOrderService {
    private final OrderRepository orderRepository;

    @Autowired
    public BinanceOrderService(OrderRepository orderRepository) {
        super();
        this.orderRepository = orderRepository;
    }

    @Override
    public void createOrder(BaseOrderResponse response, SymbolConfig symbolConfig) {
        if(response instanceof BinanceOrderResponse) {
            Order order = buildNewOrder((BinanceOrderResponse)response, symbolConfig);
            orderRepository.save(order);
            log.info("BinanceOrderService created order: {}", order);
            return;
        }
        log.warn("BinanceOrderService cannot create order because of an invalid response type {}",
                response.getClass().getName());
    }

    @Override
    public void updateOrder(BaseOrderResponse response, SymbolConfig symbolConfig) {
        if(response instanceof BinanceOrderResponse) {
            Order orgOrder = orderRepository.findByOrderId(response.getOrderId())
                    .orElseThrow();
            Order updatedOrder = buildUpdatedOrder(orgOrder, (BinanceOrderResponse)response);
            orderRepository.save(updatedOrder);
            log.info("BinanceOrderService saved order: {}", updatedOrder);
            return;
        }
        log.warn("BinanceOrderService cannot update order because of an invalid response type {}",
                response.getClass().getName());
    }

    private Order buildNewOrder(BinanceOrderResponse response, SymbolConfig symbolConfig) {
        Order order = new Order();
        order.setOrderId(response.getOrderId());
        order.setClientOrderId(response.getClientOrderId());
        order.setExchangeName(Constant.EXCHANGE_NAME_BINANCE);
        order.setSymbol(response.getSymbol());
        order.setSide(response.getSide());
        order.setPositionSide(response.getPositionSide());
        order.setEntryPrice(response.getAvgPriceAsBigDecimal());
        order.setExecutedQty(response.getExecutedQtyAsBigDecimal());
        order.setCumQuote(response.getCumQuoteAsBigDecimal());
        order.setLeverage(symbolConfig.getLeverage());
        order.setStatus(response.getStatus());
        order.setCreatedAt(response.getUpdateTimeAsInstant());
        order.setUpdatedAt(Instant.ofEpochSecond(System.currentTimeMillis()));

        return order;
    }

    private Order buildUpdatedOrder(Order orgOrder, BinanceOrderResponse response) {
        Order updatedOrder = orgOrder.toBuilder()
                .exitPrice(response.getAvgPriceAsBigDecimal())
                .closedAt(response.getUpdateTimeAsInstant())
                .updatedAt(Instant.ofEpochSecond(System.currentTimeMillis()))
                .build();
        updatedOrder.setPnl(calculatePnL(updatedOrder));
        return updatedOrder;
    }
}
