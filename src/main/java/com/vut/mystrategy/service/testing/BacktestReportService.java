package com.vut.mystrategy.service.testing;

import com.vut.mystrategy.entity.Order;
import com.vut.mystrategy.model.BacktestEquityPoint;
import com.vut.mystrategy.model.BacktestSummaryResponse;
import com.vut.mystrategy.service.OrderService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BacktestReportService {
    private final OrderService orderService;

    public BacktestReportService(OrderService orderService) {
        this.orderService = orderService;
    }

    public BacktestSummaryResponse getSummary(String exchangeName, String symbol, String klineInterval) {
        return orderService.buildBacktestSummary(exchangeName, symbol, klineInterval);
    }

    public List<Order> getOrders(String exchangeName, String symbol, String klineInterval) {
        return orderService.getOrdersByBacktestScope(exchangeName, symbol, klineInterval);
    }

    public List<BacktestEquityPoint> getEquityCurve(String exchangeName, String symbol, String klineInterval) {
        return orderService.buildEquityCurve(exchangeName, symbol, klineInterval);
    }
}
