package com.vut.mystrategy.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
public class SmaPrice extends AveragePrice implements Serializable {

    private BigDecimal topPrice;
    private BigDecimal bottomPrice;

    @Override
    public String toString() {
        return "SmaPrice(" + super.toString() + ")";
    }
}
