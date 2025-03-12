package com.vut.mystrategy.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

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

    @Column(name = "trailing_stop_percent")
    private Double trailingStopPercent;

    @Column(name = "target_profit_percent")
    private Double targetProfitPercent;

    @Column(name = "delay_millisecond")
    private Integer delayMillisecond;

    @Column(name = "default_amount")
    private String defaultAmount;

    @Column(name = "threshold")
    private BigDecimal threshold;

    @Column(name = "volume_threshold")
    private String volumeThreshold;

    @Column(name = "active")
    private boolean active = true;

    public BigDecimal[] getVolumeThresholdAsBigDecimalArray() {
        if(StringUtils.isEmpty(volumeThreshold)) {
            return new BigDecimal[] {};
        }
        return new ObjectMapper().convertValue(threshold, BigDecimal[].class);
    }
}
