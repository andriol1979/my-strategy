package com.vut.mystrategy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "trading_configs")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TradingConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange_name")
    private String exchangeName;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "stop_loss")
    private Double stopLoss; //trailing stop percent

    @Column(name = "target_profit")
    private Double targetProfit;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "sma_threshold")
    private BigDecimal smaThreshold;

    @Column(name = "ema_threshold")
    private BigDecimal emaThreshold;

    @Column(name = "divergence_threshold")
    private BigDecimal divergenceThreshold;

    @Column(name = "active")
    private boolean active = true;
}
