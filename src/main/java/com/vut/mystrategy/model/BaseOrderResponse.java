package com.vut.mystrategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@SuperBuilder
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseOrderResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("symbol")
    private String symbol;              // Cặp tiền (e.g., "BNBUSDT")

    @JsonProperty("orderId")
    private long orderId;               // ID của order trên Binance

    @JsonProperty("status")
    private String status;              // Trạng thái order (e.g., "NEW", "FILLED", "CANCELED")

    @JsonProperty("type")
    private String type;                // Loại lệnh (e.g., "LIMIT", "MARKET")

    @JsonProperty("side")
    private String side;                // Bên lệnh (e.g., "BUY", "SELL")

    @JsonProperty("positionSide")
    private String positionSide;        // Bên vị thế (Futures: "LONG", "SHORT")
}
