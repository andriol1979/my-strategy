package com.vut.mystrategy.model;

import lombok.Data;

@Data
public class StrategyRunningRequest {
    private String myStrategyMapKey;
    private String exchangeName;
    private String symbol;
    private String klineInterval;
}
