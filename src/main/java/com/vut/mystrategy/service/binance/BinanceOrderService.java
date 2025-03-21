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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Qualifier("binance")
public class BinanceOrderService extends AbstractOrderService {
    private final OrderRepository orderRepository;

    @Autowired
    public BinanceOrderService(OrderRepository orderRepository) {
        super();
        this.orderRepository = orderRepository;
    }

    @Override
    public void saveOrderToDb(Order order) {
        orderRepository.save(order);
        log.info("BinanceOrderService created order: {}", order);
    }

    @Override
    public Order buildNewOrder(BaseOrderResponse response, SymbolConfig symbolConfig) {
        if(!(response instanceof BinanceOrderResponse binanceOrderResponse)) {
            log.warn("Response type invalid {}. It should be BinanceOrderResponse",
                    response.getClass().getName());
            return null;
        }
        Order order = new Order();
        order.setOrderId(binanceOrderResponse.getOrderId());
        order.setClientOrderId(binanceOrderResponse.getClientOrderId());
        order.setExchangeName(Constant.EXCHANGE_NAME_BINANCE);
        order.setSymbol(binanceOrderResponse.getSymbol());
        order.setSide(binanceOrderResponse.getSide());
        order.setPositionSide(binanceOrderResponse.getPositionSide());
        order.setEntryPrice(binanceOrderResponse.getAvgPriceAsBigDecimal());
        order.setExecutedQty(binanceOrderResponse.getExecutedQtyAsBigDecimal());
        order.setCumQuote(binanceOrderResponse.getCumQuoteAsBigDecimal());
        order.setLeverage(symbolConfig.getLeverage());
        order.setSlippage(symbolConfig.getSlippage());
        order.setStatus(binanceOrderResponse.getStatus());
        order.setType(binanceOrderResponse.getType());
        order.setCreatedAt(binanceOrderResponse.getUpdateTime());
        order.setUpdatedAt(System.currentTimeMillis());

        return order;
    }

    @Override
    public Order buildUpdatedOrder(Order orgOrder, BaseOrderResponse response) {
        if(!(response instanceof BinanceOrderResponse binanceOrderResponse)) {
            log.warn("Response type invalid {}. It should be BinanceOrderResponse",
                    response.getClass().getName());
            return orgOrder;
        }
        Order updatedOrder = orgOrder.toBuilder()
                .exitPrice(binanceOrderResponse.getAvgPriceAsBigDecimal())
                .closedAt(binanceOrderResponse.getUpdateTime())
                .updatedAt(System.currentTimeMillis())
                .build();
        updatedOrder.setPnl(calculatePnL(updatedOrder));
        return updatedOrder;
    }
}
