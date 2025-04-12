package com.vut.mystrategy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "backtest_data")
@NoArgsConstructor
@AllArgsConstructor
public class BacktestDatum implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "event_time")
    private Instant eventTime;

    @Column(name = "open")
    private BigDecimal open;

    @Column(name = "high")
    private BigDecimal high;

    @Column(name = "low")
    private BigDecimal low;

    @Column(name = "close")
    private BigDecimal close;

    @Column(name = "volume")
    private BigDecimal volume;

    @Size(max = 255)
    @Column(name = "exchange_name")
    private String exchangeName;

    @Size(max = 255)
    @Column(name = "symbol")
    private String symbol;

    @Size(max = 5)
    @Column(name = "kline_interval", length = 5)
    private String klineInterval;

}