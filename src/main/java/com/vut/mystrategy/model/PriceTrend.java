package com.vut.mystrategy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceTrend {
    private String key;                         // gen unique key from utility class -> it's HashMap key
    private String symbol;                      // BNBUSDT
    private String trend;                       // UP/DOWN
    private int level;                          // 1,2,3,4,5
    private String suggestion;                  // BUY/SELL or NOT ...
    private Long timestamp;
}
