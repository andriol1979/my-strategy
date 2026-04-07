package com.vut.mystrategy.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BacktestSummaryResponse {
    private String exchangeName;
    private String symbol;
    private String klineInterval;
    private int totalTrades;
    private int winningTrades;
    private int losingTrades;
    private BigDecimal winRate;
    private BigDecimal totalPnl;
    private BigDecimal averagePnl;
    private BigDecimal averageWinPnl;
    private BigDecimal averageLossPnl;
    private BigDecimal profitFactor;
    private BigDecimal maxDrawdown;
    private BigDecimal maxDrawdownRate;
    private Long firstOrderCreatedAt;
    private Long lastOrderClosedAt;
}
