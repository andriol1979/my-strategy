package com.vut.mystrategy.model.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vut.mystrategy.model.BaseOrderResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceOrderResponse extends BaseOrderResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
    private String cumQuote;     // Tổng giá trị quote đã khớp (Futures)

    @JsonProperty("reduceOnly")
    private Boolean reduceOnly;

    @JsonProperty("timeInForce")
    private String timeInForce;         // Thời gian hiệu lực (e.g., "GTC", "IOC")

    @JsonProperty("transactTime")
    private Long transactTime;          // Thời gian giao dịch (Spot)

    @JsonProperty("updateTime")
    private Long updateTime;            // Thời gian cập nhật (Futures)

    @JsonProperty("stopPrice")
    private String stopPrice;

    @JsonProperty("workingType") //"CONTRACT_PRICE"
    private String workingType;

    @JsonProperty("activatePrice") //"0"
    private String activatePrice;

    @JsonProperty("priceRate") //"0"
    private String priceRate;

    @JsonProperty("avgPrice") // "51023.456789"
    private String avgPrice;

    @JsonProperty("origType") // "MARKET"
    private String origType;

    @JsonProperty("fills")
    private List<FillResponse> fills;           // Danh sách các lần khớp lệnh (Spot)

    public BigDecimal getAvgPriceAsBigDecimal() {
        return new BigDecimal(avgPrice);
    }

    public BigDecimal getExecutedQtyAsBigDecimal() {
        return new BigDecimal(executedQuantity);
    }

    public BigDecimal getCumQuoteAsBigDecimal() {
        return new BigDecimal(cumQuote);
    }
}

/*
{
  "symbol": "BTCUSDT",
  "orderId": 123456789,
  "clientOrderId": "6gCrw2kRUAF9CvJDx2vC5g",
  "price": "0",              // Giá đặt lệnh, với Market Order là "0" (dùng giá thị trường)
  "origQty": "0.01000000",   // Số lượng đặt ban đầu
  "executedQty": "0.01000000", // Số lượng đã khớp
  "cumQuote": "500.12345678",  // Tổng giá trị đã khớp (quote asset, ví dụ USDT)
  "reduceOnly": false,        // Có phải lệnh giảm vị thế không
  "status": "FILLED",         // Trạng thái: NEW, PARTIALLY_FILLED, FILLED, CANCELED, etc.
  "timeInForce": "GTC",       // Thời gian hiệu lực (GTC, IOC, FOK), với Market thường mặc định GTC
  "type": "MARKET",           // Loại lệnh
  "side": "BUY",              // BUY hoặc SELL
  "stopPrice": "0",           // Giá stop (0 nếu không dùng)
  "workingType": "CONTRACT_PRICE", // Loại giá áp dụng (thường mặc định)
  "activatePrice": "0",       // Giá kích hoạt (dùng cho Trailing Stop)
  "priceRate": "0",           // Callback rate (dùng cho Trailing Stop)
  "updateTime": 1698765432100, // Thời gian cập nhật (Unix timestamp, ms)
  "avgPrice": "50012.345678", // Giá trung bình khớp lệnh
  "origType": "MARKET",       // Loại lệnh gốc
  "positionSide": "BOTH"      // BOTH (Hedge Mode), LONG, SHORT
}
 */
