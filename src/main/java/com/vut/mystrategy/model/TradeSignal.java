package com.vut.mystrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeSignal implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String exchangeName;
    private String symbol;
    private String side;         // "BUY" hoặc "SELL"
    private String positionSide; // "LONG" hoặc "SHORT"
    private BigDecimal price;    // current market price to buy or sell
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private String action; //ENTRY-LONG, EXIT-LONG, ENTRY-SHORT, EXIT-SHORT

    private long timestamp;
}
