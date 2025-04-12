package com.vut.mystrategy.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class OrderResponseStorage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private BaseOrderResponse entryResponse;
    private BaseOrderResponse exitResponse;
}
