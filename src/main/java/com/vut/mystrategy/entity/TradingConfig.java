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
    private BigDecimal smaThreshold; //compare with smaTrendStrength 0.04 ~ 0.06 yếu, 0.1 ok

    @Column(name = "ema_threshold")
    private BigDecimal emaThreshold; //Ngưỡng chênh lệch giữa EMA(5) và EMA(10) để xác nhận crossover đáng tin cậy

    @Column(name = "divergence_threshold")
    private BigDecimal divergenceThreshold; //compare with volumeChangePercent(newTotalVolume and prevTotalVolume) to decide volume UP or DOWN: 10%

    @Column(name = "volume_threshold")
    private BigDecimal volumeThreshold; //calculate volume spike bull volume > 1.5 * bear volume

    @Column(name = "price_threshold")
    private BigDecimal priceThreshold;  //calculate market price is near resistance or near support: 0.01 ~ 0.05

    @Column(name = "max_concurrent_orders")
    private Integer maxConcurrentOrders;

    @Column(name = "active")
    private boolean active = true;
}
//Note: all threshold values is % Ex: 01.%
