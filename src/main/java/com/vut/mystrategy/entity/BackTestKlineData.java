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

@Getter
@Setter
@Entity
@Table(name = "backtest_kline_data")
@NoArgsConstructor
@AllArgsConstructor
public class BackTestKlineData  implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id; // Thêm ID giả định nếu bảng không có primary key

    @Column(name = "open_time")
    private Long openTime;

    @Column(name = "close_time")
    private Long closeTime;

    @Column(name = "open", precision = 18, scale = 8)
    private BigDecimal open;

    @Column(name = "high", precision = 18, scale = 8)
    private BigDecimal high;

    @Column(name = "low", precision = 18, scale = 8)
    private BigDecimal low;

    @Column(name = "close", precision = 18, scale = 8)
    private BigDecimal close;

    @Column(name = "volume", precision = 18, scale = 8)
    private BigDecimal volume;

    @Column(name = "quote_volume", precision = 18, scale = 8)
    private BigDecimal quoteVolume;

    @Column(name = "count")
    private Integer count;

    @Column(name = "taker_buy_volume", precision = 18, scale = 8)
    private BigDecimal takerBuyVolume;

    @Column(name = "taker_buy_quote_volume", precision = 18, scale = 8)
    private BigDecimal takerBuyQuoteVolume;

    @Column(name = "ignore")
    private Boolean ignore;

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
