package com.vut.mystrategy.model.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FillResponse {
    @JsonProperty("price")
    private String price;               // Giá khớp

    @JsonProperty("qty")
    private String quantity;            // Số lượng khớp

    @JsonProperty("commission")
    private String commission;          // Phí giao dịch

    @JsonProperty("commissionAsset")
    private String commissionAsset;     // Đơn vị phí (e.g., "BNB")

    @JsonProperty("tradeId")
    private long tradeId;               // ID giao dịch
}
