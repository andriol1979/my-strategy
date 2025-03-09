package com.vut.mystrategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceFutureLotSizeResponse {

    private String symbol;

    @JsonProperty("minQty")
    private String minQty;

    @JsonProperty("stepSize")
    private String stepSize;

    @JsonProperty("maxQty")
    private String maxQty;

    @JsonProperty("filterType")
    private String filterType;

    public BigDecimal getStepSizeAsBigDecimal() {
        return new BigDecimal(stepSize);
    }
}
