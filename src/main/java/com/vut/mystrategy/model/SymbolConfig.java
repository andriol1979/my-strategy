package com.vut.mystrategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SymbolConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @JsonProperty(value = "exchangeName", required = true)
    @NotBlank(message = "Exchange is required and cannot be empty. Default is binance")
    private String exchangeName;
    @JsonProperty(value = "symbol", required = true)
    @NotBlank(message = "Symbol is required and cannot be empty. Valid symbol is bnbusdt")
    private String symbol;
    private BigDecimal stopLoss; // stop loss, 0.005. Ex: price BNB x 0.005 = 3.25
    private Double targetProfit; //take profit 0.005
    private BigDecimal orderVolume; //unit based on USDT. Ex 8 USDT / order
    private BigDecimal smaThreshold; //compare with analyzeSmaTrendLevelBySlope to decide UP/DOWN 0.04 ~ 0.06 yếu, 0.1 ok
    private BigDecimal smaTrendStrengthThreshold; //use to compare with smaTrendStrength to decide smaTrendIsBullish
    private BigDecimal resistanceThreshold; //dùng để đo có vượt qua ngưỡng trong 2 method: priceIsNearResistance, priceIsUpOverResistance
    private BigDecimal supportThreshold; //dùng để đo có vượt qua ngưỡng trong 2 method: priceIsNearSupport, priceIsDownUnderSupport
    private BigDecimal emaThresholdAbove; //Ngưỡng chênh lệch trên: lúc EMA(5) cắt lên EMA(10) bao nhiêu USDT để xác nhận crossover đáng tin cậy
    private BigDecimal emaThresholdBelow; //Ngưỡng chênh lệch dưới: lúc EMA(5) cắt xuống EMA(10) bao nhiêu USDT để xác nhận crossover đáng tin cậy
    private BigDecimal divergenceThreshold; //compare with volumeChangePercent(newTotalVolume and prevTotalVolume) to decide volume UP or DOWN: 10%
    private BigDecimal volumeThreshold; //calculate volume spike bull volume > 1.5 * bear volume
    private BigDecimal priceThreshold;  //calculate market price is near resistance or near support: 0.01 ~ 0.05
    private Integer maxConcurrentOrders;
    private boolean active = true;
    /*
     New: 20.Mar.2025
        move all config in application.properties into symbol config
        -> so we can use the configuration values for separate symbol
     */

    // group-size=5: group 5 trade_event to calculate SMA
    // sma-period=10
    private Integer smaPeriod;

    // window number to calculate smoothing factor 2 / (ema-short-period + 1) = 0.3333
    //ema-short-period=5
    private Integer emaShortPeriod;
    // ema-long-period=10
    private Integer emaLongPeriod;

    // millisecond = 25s
    // sum-volume-period=5000
    private Integer sumVolumePeriod;
    // taker volume contributes 60% to total volume
    //sum-volume-taker-weight=0.6
    private BigDecimal sumVolumeTakerWeight;
    // maker volume contributes 40% to total volume
    // sum-volume-maker-weight=0.4
    private BigDecimal sumVolumeMakerWeight;

    // Configuration parameters to identity market trending
    // 5 chu kì SMA liên tiep -> tính SMA trend -> tính resistance & support
    // base-trend-sma-period=5
    private Integer baseTrendSmaPeriod;

    // 2 = số sumVolume được lấy để tính
    // base-trend-divergence-volume-period=3
    private Integer baseTrendDivergenceVolumePeriod;

    //Tỷ lệ trượt giá đã chia 100. Ex: 1% = 0.01
    private BigDecimal slippage;

    //MIN_VOLUME_STRENGTH_THRESHOLD = 4
    private Integer minVolumeStrengthThreshold;

    //fetch data - trading signal jobs delay time in millisecond: 500
    private Long fetchDataDelayTime;

    //Đòn bẩy: 5x
    private Integer leverage;
}
//Note: all threshold values is divided 100
