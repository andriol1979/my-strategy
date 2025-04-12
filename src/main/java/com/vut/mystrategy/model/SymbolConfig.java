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
    private List<String> feedKlineIntervals; //["1m", "5m] -> feed data từ binance với những loại kline này

    // window number to calculate smoothing factor 2 / (ema-short-period + 1) = 0.3333
    //ema-short-period=9
    private Integer emaShortPeriod;
    // ema-long-period=21
    private Integer emaLongPeriod;
    // xác định xu hướng dài: 200
    private Integer emaLongTermPeriod;
    private BigDecimal orderVolume; //unit based on USDT. Ex 8 USDT / order
    private String strategyName;
    //taker buy < taker sell 5%
    private Double buyUnderSellVolumePercentage;
    //taker buy > taker sell 5%
    private Double buyOverSellVolumePercentage;

    private BigDecimal resistanceThreshold; //dùng để đo có vượt qua ngưỡng trong 2 method: priceIsNearResistance, priceIsUpOverResistance
    private BigDecimal supportThreshold; //dùng để đo có vượt qua ngưỡng trong 2 method: priceIsNearSupport, priceIsDownUnderSupport
    //calculate market price is near resistance or near support: 0.01 ~ 0.05
    private BigDecimal priceThreshold;

    //compare with ratio between bull / bear (số nhỏ /số lớn) giá trị luôn từ 0 -> nhỏ hơn 1
    //Ex: 0.65 -> số nhỏ có t lệ bằng 65% số lớn
    private BigDecimal volumeThreshold;

    private boolean active = true;
    //Tỷ lệ trượt giá đã chia 100. Ex: 1% = 0.01
    private BigDecimal slippage;
    //Đòn bẩy: 5x
    private Integer leverage;
}
//Note: all threshold values is divided 100
