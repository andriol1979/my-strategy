package com.vut.mystrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AveragePrice implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String exchangeName;
    private String symbol;
    private BigDecimal price;
    private long timestamp;
}
