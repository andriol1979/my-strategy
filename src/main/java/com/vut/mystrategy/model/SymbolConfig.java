package com.vut.mystrategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

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
    private String emaKlineInterval; //chọn loại kline nào để tính EMA. Vd: EMA 9 và EMA 21, trên khung 5 phút -> emaKlineInterval = 5m
    private String smaKlineInterval; //chọn loại kline nào để tính SMA. Vd: SMA 50, trên khung 5 phút -> smaKlineInterval = 5m (nên = emaKlineInterval)
    private List<String> feedKlineIntervals; //["1m", "5m] -> feed data từ binance với những loại kline này
    // sma-period=30
    private Integer smaPeriod;

    // window number to calculate smoothing factor 2 / (ema-short-period + 1) = 0.3333
    //ema-short-period=9
    private Integer emaShortPeriod;
    // ema-long-period=21
    private Integer emaLongPeriod;




    private BigDecimal orderVolume; //unit based on USDT. Ex 8 USDT / order
    private BigDecimal smaThreshold; //compare with analyzeSmaTrendLevelBySlope to decide UP/DOWN 0.04 ~ 0.06 yếu, 0.1 ok
    private BigDecimal smaTrendStrengthThreshold; //use to compare with smaTrendStrength to decide smaTrendIsBullish
    private BigDecimal resistanceThreshold; //dùng để đo có vượt qua ngưỡng trong 2 method: priceIsNearResistance, priceIsUpOverResistance
    private BigDecimal supportThreshold; //dùng để đo có vượt qua ngưỡng trong 2 method: priceIsNearSupport, priceIsDownUnderSupport
    private BigDecimal emaThresholdAbove; //Ngưỡng chênh lệch trên: lúc EMA(5) cắt lên EMA(10) bao nhiêu USDT để xác nhận crossover đáng tin cậy
    private BigDecimal emaThresholdBelow; //Ngưỡng chênh lệch dưới: lúc EMA(5) cắt xuống EMA(10) bao nhiêu USDT để xác nhận crossover đáng tin cậy

    //compare with currDivergence and prevDivergence to analyze volume trend strength point in TradingSignalAnalyzer
    //unit = %. Ex: 10, 12.5, 20, 25...
    private BigDecimal divergenceThreshold;

    //compare with ratio between bull / bear (số nhỏ /số lớn) giá trị luôn từ 0 -> nhỏ hơn 1
    //Ex: 0.65 -> số nhỏ có t lệ bằng 65% số lớn
    private BigDecimal volumeThreshold;

    //calculate market price is near resistance or near support: 0.01 ~ 0.05
    private BigDecimal priceThreshold;
    private Integer maxConcurrentOrders;
    private boolean active = true;
    /*
     New: 20.Mar.2025
        move all config in application.properties into symbol config
        -> so we can use the configuration values for separate symbol
     */

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
