package com.vut.mystrategy.model.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeLiteEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String e;   // Event type
    private long E;     // Event time
    private long T;     // Transaction time
    private String s;   // Symbol
    private String q;   // Quantity
    private String p;   // Price
    private boolean m;  // Is buyer the market maker
    private String c;   // Client order ID
    private String S;   // Side (BUY/SELL)
    private String L;   // Last price
    private String l;   // Last quantity
    private long t;     // Trade ID
    private long i;     // Order ID
}
