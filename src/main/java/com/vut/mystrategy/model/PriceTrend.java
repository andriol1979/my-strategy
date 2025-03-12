package com.vut.mystrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceTrend implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String exchangeName;
    private String symbol;                      // BNBUSDT
    private String trend;                       // UP/DOWN
    private int level;                          // 1,2,3,4,5
    private BigDecimal currAvgPrice;            // currAvgPrice to be used to calculate
    private BigDecimal prevAvgPrice;            // prevAvgPrice to be used to calculate
    private BigDecimal strength;                // % change: Ex: UP 3%, DOWN 5%
    private String suggestion;                  // BUY/SELL or NOT ...
    private Long timestamp;

    public static PriceTrend buildSimplePriceTrend(String exchangeName, String symbol,
                                                   BigDecimal currAvg, BigDecimal prevAvg) {
        return PriceTrend.builder()
                .exchangeName(exchangeName)
                .symbol(symbol)
                .currAvgPrice(currAvg)
                .prevAvgPrice(prevAvg)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
