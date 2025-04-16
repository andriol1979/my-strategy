package com.vut.mystrategy.model.binance;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ListenKeyResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String listenKey;
}
