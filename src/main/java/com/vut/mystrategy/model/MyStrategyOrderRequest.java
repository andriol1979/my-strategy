package com.vut.mystrategy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vut.mystrategy.helper.Constant;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyStrategyOrderRequest {
    @JsonProperty(value = "exchangeName", required = true)
    @NotBlank(message = "Exchange is required and cannot be empty. Default is binance")
    private String exchangeName = Constant.EXCHANGE_NAME_BINANCE;
    @JsonProperty(value = "symbol", required = true)
    @NotBlank(message = "Symbol is required and cannot be empty. Valid symbol is bnbusdt")
    private String symbol;              // Cặp tiền (e.g., "BNBUSDT")
    @JsonProperty(value = "side", required = true)
    @NotBlank(message = "Side is required and cannot be empty. BUY/SELL")
    private String side;                // Bên lệnh (e.g., "BUY", "SELL")
    @JsonProperty("positionSide")
    private String positionSide;        // Vị thế (Futures: "LONG", "SHORT")
    @JsonProperty("price")
    private BigDecimal price;           // Giá (cho LIMIT order, tùy chọn)
}
