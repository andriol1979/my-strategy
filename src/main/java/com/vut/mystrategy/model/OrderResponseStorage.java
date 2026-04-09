package com.vut.mystrategy.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class OrderResponseStorage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private BaseOrderResponse entryResponse;
    private BaseOrderResponse exitResponse;
    private Integer dcaCount;
    private Integer initialEntryIndex;
    private Integer lastEntryIndex;
    private Long initialEntryTime;
    private Long lastEntryTime;
    private BigDecimal peakPrice;
    private BigDecimal trailingStopPrice;
    private Boolean trailingActive;
    private String exitReason;
}
