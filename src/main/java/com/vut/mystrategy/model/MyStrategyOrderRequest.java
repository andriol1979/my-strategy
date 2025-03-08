package com.vut.mystrategy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.Utility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyStrategyOrderRequest {
    @JsonProperty("orderId")
    private String orderId = Utility.generateOrderId(); //auto generate
    @JsonProperty("orderStatus")
    private String orderStatus = Constant.ORDER_STATUS_WAIT;
    @JsonProperty("symbol")
    private String symbol;              // Cặp tiền (e.g., "BNBUSDT")
    @JsonProperty("type")
    private String type = Constant.ORDER_TYPE_MARKET;  // Loại lệnh (e.g., "LIMIT", "MARKET")
    @JsonProperty("positionSide")
    private String positionSide;        // Vị thế (Futures: "LONG", "SHORT")
    @JsonProperty("price")
    private String price;               // Giá (cho LIMIT order, tùy chọn)
    @JsonProperty("quantity")
    private String quantity;            // Số lượng (chuỗi để giữ precision)
}
