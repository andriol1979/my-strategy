package com.vut.mystrategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LotSizeResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("stepSize")
    private String stepSize;

    public BigDecimal getStepSizeAsBigDecimal() {
        return new BigDecimal(stepSize);
    }
}
