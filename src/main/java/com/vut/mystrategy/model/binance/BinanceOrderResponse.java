package com.vut.mystrategy.model.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceOrderResponse {
    @JsonProperty("symbol")
    private String symbol;              // Cặp tiền (e.g., "BNBUSDT")

    @JsonProperty("orderId")
    private long orderId;               // ID của order trên Binance

    @JsonProperty("clientOrderId")
    private String clientOrderId;       // ID do bot tạo (nếu có)

    @JsonProperty("price")
    private String price;               // Giá đặt lệnh (chuỗi để giữ precision)

    @JsonProperty("origQty")
    private String originalQuantity;    // Số lượng đặt ban đầu

    @JsonProperty("executedQty")
    private String executedQuantity;    // Số lượng đã khớp

    @JsonProperty("cummulativeQuoteQty")
    private String cumulativeQuoteQty;  // Tổng giá trị quote đã khớp (Spot)

    @JsonProperty("cumQuote")
    private String cumulativeQuote;     // Tổng giá trị quote đã khớp (Futures)

    @JsonProperty("status")
    private String status;              // Trạng thái order (e.g., "NEW", "FILLED", "CANCELED")

    @JsonProperty("timeInForce")
    private String timeInForce;         // Thời gian hiệu lực (e.g., "GTC", "IOC")

    @JsonProperty("type")
    private String type;                // Loại lệnh (e.g., "LIMIT", "MARKET")

    @JsonProperty("side")
    private String side;                // Bên lệnh (e.g., "BUY", "SELL")

    @JsonProperty("positionSide")
    private String positionSide;        // Bên vị thế (Futures: "LONG", "SHORT")

    @JsonProperty("transactTime")
    private Long transactTime;          // Thời gian giao dịch (Spot)

    @JsonProperty("updateTime")
    private Long updateTime;            // Thời gian cập nhật (Futures)

    @JsonProperty("fills")
    private List<FillResponse> fills;           // Danh sách các lần khớp lệnh (Spot)
}
