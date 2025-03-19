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
    private BigDecimal emaThreshold; //Ngưỡng chênh lệch giữa EMA(5) và EMA(10) để xác nhận crossover đáng tin cậy
    private BigDecimal divergenceThreshold; //compare with volumeChangePercent(newTotalVolume and prevTotalVolume) to decide volume UP or DOWN: 10%
    private BigDecimal volumeThreshold; //calculate volume spike bull volume > 1.5 * bear volume
    private BigDecimal priceThreshold;  //calculate market price is near resistance or near support: 0.01 ~ 0.05
    private Integer maxConcurrentOrders;
    private boolean active = true;

    public SymbolConfig(SymbolConfig target) {
        this.exchangeName = target.getExchangeName();
        this.symbol = target.getSymbol();
        this.stopLoss = target.getStopLoss();
        this.targetProfit = target.getTargetProfit();
        this.orderVolume = target.getOrderVolume();
        this.smaThreshold = target.getSmaThreshold();
        this.emaThreshold = target.getEmaThreshold();
        this.divergenceThreshold = target.getDivergenceThreshold();
        this.volumeThreshold = target.getVolumeThreshold();
        this.priceThreshold = target.getPriceThreshold();
        this.maxConcurrentOrders = target.getMaxConcurrentOrders();
        this.active = target.isActive();
    }
}
//Note: all threshold values is divided 100
