package com.vut.mystrategy.model.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vut.mystrategy.model.LotSizeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceFutureLotSizeResponse extends LotSizeResponse {

    @JsonProperty("minQty")
    private String minQty;

    @JsonProperty("maxQty")
    private String maxQty;

    @JsonProperty("filterType")
    private String filterType;
}
