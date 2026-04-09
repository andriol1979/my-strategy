package com.vut.mystrategy.service;

import com.vut.mystrategy.entity.Order;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.BacktestEquityPoint;
import com.vut.mystrategy.model.BacktestSummaryResponse;
import com.vut.mystrategy.model.*;
import com.vut.mystrategy.model.binance.BinanceOrderResponse;
import com.vut.mystrategy.repository.OrderRepository;
import com.vut.mystrategy.service.order.binance.BinanceOrderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class OrderService {
    private final OrderRepository orderRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Async("jpaTaskAsync")
    public void buildAndSaveOrder(OrderResponseStorage orderResponseStorage, SymbolConfig symbolConfig) {
        if(!validateOrderResponseStorage(orderResponseStorage)) {
            return;
        }
        Order order = null;
        if(symbolConfig.getExchangeName().equals(Constant.EXCHANGE_NAME_BINANCE)) {
            BinanceOrderResponse entryResponse = orderResponseStorage.getEntryResponse().as(BinanceOrderResponse.class);
            BinanceOrderResponse exitResponse = orderResponseStorage.getExitResponse().as(BinanceOrderResponse.class);
            order = BinanceOrderBuilder.buildOrder(entryResponse, exitResponse, symbolConfig, orderResponseStorage.getExitReason());
        }
        //TODO: more exchange here
        //.....

        // save order to postgres
        if(order != null) {
            orderRepository.save(order);
            log.info("OrderService created order: {}", order);
            return;
        }
        log.warn("Cannot save order because order is null");
    }

    @Transactional
    public long deleteOrdersByBacktestScope(String exchangeName, String symbol, String klineInterval) {
        long deleted = orderRepository.deleteByExchangeNameAndSymbolAndKlineInterval(exchangeName, symbol, klineInterval);
        log.info("Deleted {} order(s) for {} {} {}", deleted, exchangeName, symbol, klineInterval);
        return deleted;
    }

    public List<Order> getOrdersByBacktestScope(String exchangeName, String symbol, String klineInterval) {
        return orderRepository.findByExchangeNameAndSymbolAndKlineIntervalOrderByCreatedAtAsc(exchangeName, symbol, klineInterval);
    }

    public BacktestSummaryResponse buildBacktestSummary(String exchangeName, String symbol, String klineInterval) {
        List<Order> orders = getOrdersByBacktestScope(exchangeName, symbol, klineInterval);
        List<BacktestEquityPoint> equityCurve = buildEquityCurve(orders);
        int totalTrades = orders.size();
        long winningTrades = orders.stream()
                .filter(order -> order.getPnl() != null && order.getPnl().compareTo(BigDecimal.ZERO) > 0)
                .count();
        long losingTrades = orders.stream()
                .filter(order -> order.getPnl() != null && order.getPnl().compareTo(BigDecimal.ZERO) < 0)
                .count();

        BigDecimal totalPnl = orders.stream()
                .map(Order::getPnl)
                .filter(pnl -> pnl != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossProfit = orders.stream()
                .map(Order::getPnl)
                .filter(pnl -> pnl != null && pnl.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossLoss = orders.stream()
                .map(Order::getPnl)
                .filter(pnl -> pnl != null && pnl.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averagePnl = average(totalPnl, totalTrades);
        BigDecimal averageWinPnl = average(grossProfit, winningTrades);
        BigDecimal averageLossPnl = average(grossLoss, losingTrades);
        BigDecimal winRate = totalTrades == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(winningTrades)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalTrades), 2, RoundingMode.HALF_UP);
        BigDecimal profitFactor = grossLoss.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : grossProfit.divide(grossLoss, 4, RoundingMode.HALF_UP);
        BigDecimal maxDrawdown = equityCurve.stream()
                .map(BacktestEquityPoint::getDrawdown)
                .reduce(BigDecimal.ZERO, BigDecimal::max);
        BigDecimal maxDrawdownRate = equityCurve.stream()
                .map(BacktestEquityPoint::getDrawdownRate)
                .reduce(BigDecimal.ZERO, BigDecimal::max);
        String mostCommonExitReason = orders.stream()
                .map(Order::getExitReason)
                .filter(exitReason -> exitReason != null && !exitReason.isBlank())
                .collect(java.util.stream.Collectors.groupingBy(exitReason -> exitReason, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);

        return BacktestSummaryResponse.builder()
                .exchangeName(exchangeName)
                .symbol(symbol)
                .klineInterval(klineInterval)
                .totalTrades(totalTrades)
                .winningTrades((int) winningTrades)
                .losingTrades((int) losingTrades)
                .winRate(winRate)
                .totalPnl(totalPnl)
                .averagePnl(averagePnl)
                .averageWinPnl(averageWinPnl)
                .averageLossPnl(averageLossPnl)
                .profitFactor(profitFactor)
                .maxDrawdown(maxDrawdown)
                .maxDrawdownRate(maxDrawdownRate)
                .mostCommonExitReason(mostCommonExitReason)
                .firstOrderCreatedAt(orders.stream().map(Order::getCreatedAt).min(Comparator.naturalOrder()).orElse(null))
                .lastOrderClosedAt(orders.stream().map(Order::getClosedAt).filter(closedAt -> closedAt != null).max(Comparator.naturalOrder()).orElse(null))
                .build();
    }

    public List<BacktestEquityPoint> buildEquityCurve(String exchangeName, String symbol, String klineInterval) {
        return buildEquityCurve(getOrdersByBacktestScope(exchangeName, symbol, klineInterval));
    }

    private boolean validateOrderResponseStorage(OrderResponseStorage orderResponseStorage) {
        if(orderResponseStorage == null) {
            log.error("OrderResponseStorage is null");
            return false;
        }
        if(orderResponseStorage.getEntryResponse() == null) {
            log.error("OrderResponseStorage.EntryResponse is null");
            return false;
        }
        if(orderResponseStorage.getExitResponse() == null) {
            log.error("OrderResponseStorage.ExitResponse is null");
            return false;
        }
        return true;
    }

    private BigDecimal average(BigDecimal total, long divisor) {
        if (divisor <= 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(divisor), 8, RoundingMode.HALF_UP);
    }

    private List<BacktestEquityPoint> buildEquityCurve(List<Order> orders) {
        List<BacktestEquityPoint> equityCurve = new ArrayList<>();
        BigDecimal cumulativePnl = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;

        for (int index = 0; index < orders.size(); index++) {
            Order order = orders.get(index);
            BigDecimal pnl = order.getPnl() == null ? BigDecimal.ZERO : order.getPnl();
            cumulativePnl = cumulativePnl.add(pnl);
            if (cumulativePnl.compareTo(peak) > 0) {
                peak = cumulativePnl;
            }

            BigDecimal drawdown = peak.subtract(cumulativePnl).max(BigDecimal.ZERO);
            BigDecimal drawdownRate = peak.compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ZERO
                    : drawdown.multiply(BigDecimal.valueOf(100)).divide(peak, 4, RoundingMode.HALF_UP);

            equityCurve.add(BacktestEquityPoint.builder()
                    .tradeNumber(index + 1)
                    .time(order.getClosedAt() != null ? order.getClosedAt() : order.getCreatedAt())
                    .pnl(pnl)
                    .cumulativePnl(cumulativePnl)
                    .drawdown(drawdown)
                    .drawdownRate(drawdownRate)
                    .exitReason(order.getExitReason())
                    .build());
        }

        return equityCurve;
    }
}
