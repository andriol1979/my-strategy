package com.vut.mystrategy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolumeTrend {
    private String key;                         // gen unique key from utility class -> it's HashMap key
    private String exchangeName;
    private String symbol;                      // BNBUSDT
    private BigDecimal buyVolume;
    private BigDecimal sellVolume;
    private String dominantSide;                // BUY/SELL
    private int buyVolumeLevel;                 // 1,2,3,4,5...
    private int sellVolumeLevel;                // 1,2,3,4,5...
    private String suggestion;
    private Long timestamp;
}
