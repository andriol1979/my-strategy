package com.vut.mystrategy.model.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderTradeUpdateEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String e;      // Event type
    private long T;        // Transaction time
    private long E;        // Event time
    private Order o;       // Order details

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Order {
        private String s, c, S, o, f, q, p, ap, sp, x, X, l, z, L, n, N, wt, ot, ps, rp, V, pm;
        private long i, T, t, si, ss, gtd;
        private boolean m, R, cp, pP;
        private String b, a;
    }
}
