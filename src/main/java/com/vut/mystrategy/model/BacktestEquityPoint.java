package com.vut.mystrategy.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BacktestEquityPoint {
    private int tradeNumber;
    private Long time;
    private BigDecimal pnl;
    private BigDecimal cumulativePnl;
    private BigDecimal drawdown;
    private BigDecimal drawdownRate;
}
