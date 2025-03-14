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
public class SmaTrend implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String exchangeName;
    private String symbol;

    // 2 fields are calculated based on 5 SMA periods -> triggered by SMA calculator
    private BigDecimal resistancePrice;
    private BigDecimal supportPrice;
    private int smaTrendLevel; //positive = UP - negative = DOWN
    private BigDecimal smaTrendStrength;

    private long timestamp;
}
