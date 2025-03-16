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
public class VolumeTrend implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String exchangeName;
    private String symbol;

    private String currTrendDirection;
    private String prevTrendDirection;

    private BigDecimal currDivergence; //second sum volume -> become third sum volume -> receive the newest sum volume value
    private BigDecimal prevDivergence; //third sum volume -> remove out -> receive second sum volume value

    private BigDecimal currTrendStrength;
    private BigDecimal prevTrendStrength;

    private String volumeSpike;

    private long timestamp;
}
