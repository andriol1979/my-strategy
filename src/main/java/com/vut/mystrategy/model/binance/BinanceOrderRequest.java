package com.vut.mystrategy.model.binance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BinanceOrderRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @JsonProperty("symbol")
    private String symbol;              // Cặp tiền (e.g., "BNBUSDT")

    @JsonProperty("side")
    private String side;                // Bên lệnh (e.g., "BUY", "SELL")

    @JsonProperty("type")
    private String type;                // Loại lệnh (e.g., "LIMIT", "MARKET")

    @JsonProperty("quantity")
    private String quantity;            // Số lượng (chuỗi để giữ precision)

    @JsonProperty("price")
    private String price;               // Giá (cho LIMIT order, tùy chọn)

    @JsonProperty("timeInForce")
    private String timeInForce;         // Thời gian hiệu lực (e.g., "GTC", "IOC")

    @JsonProperty("newClientOrderId")
    private String newClientOrderId;    // ID do bot tạo (tùy chọn)

    @JsonProperty("positionSide")
    private String positionSide;        // Vị thế (Futures: "LONG", "SHORT")

    @JsonProperty("timestamp")
    private Long timestamp = System.currentTimeMillis();
}
