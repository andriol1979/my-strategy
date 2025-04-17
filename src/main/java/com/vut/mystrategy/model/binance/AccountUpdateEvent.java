package com.vut.mystrategy.model.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountUpdateEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String e;   // Event type
    private long T;     // Transaction time
    private long E;     // Event time
    private Account a;  // Account info

    // Inner class: Account
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Account {
        private List<Balance> B;  // Balances
        private List<Position> P; // Positions
        private String m;         // Event reason type (e.g., ORDER)
    }

    // Inner class: Balance
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Balance {
        private String a;  // Asset
        private String wb; // Wallet balance
        private String cw; // Cross wallet balance
        private String bc; // Balance change
    }

    // Inner class: Position
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Position {
        private String s;    // Symbol
        private String pa;   // Position amount
        private String ep;   // Entry price
        private String cr;   // Accumulated realized
        private String up;   // Unrealized PnL
        private String mt;   // Margin type
        private String iw;   // Isolated wallet
        private String ps;   // Position side
        private String ma;   // Margin asset
        private String bep;  // Break-even price
    }
}
