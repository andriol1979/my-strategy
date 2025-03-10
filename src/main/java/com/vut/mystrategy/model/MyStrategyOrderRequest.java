package com.vut.mystrategy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyStrategyOrderRequest {
    @JsonProperty("symbol")
    private String symbol;              // Cặp tiền (e.g., "BNBUSDT")
    @JsonProperty("positionSide")
    private String positionSide;        // Vị thế (Futures: "LONG", "SHORT")
    @JsonProperty("price")
    private BigDecimal price;               // Giá (cho LIMIT order, tùy chọn)
}
