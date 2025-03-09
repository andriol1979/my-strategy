package com.vut.mystrategy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Table(name = "trading_configs")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TradingConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "trailing_stop_percent")
    private double trailingStopPercent;

    @Column(name = "target_profit_percent")
    private double targetProfitPercent;

    @Column(name = "delay_millisecond")
    private int delayMillisecond;

    @Column(name = "active")
    private boolean active = true;
}
