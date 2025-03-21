package com.vut.mystrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolumeTrend implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String exchangeName;
    private String symbol;

    private String currTrendDirection;
    private String prevTrendDirection;

    //second sum volume -> become third sum volume -> receive the newest sum volume value
    //positive -> UP:   0 -> 100
    //negative -> DOWN: 0 -> -100
    private BigDecimal currDivergence;
    //third sum volume -> remove out -> receive second sum volume value
    //positive -> UP:   0 -> 100
    //negative -> DOWN: 0 -> -100
    private BigDecimal prevDivergence;

    private BigDecimal newTotalVolume; //new bull volume + new bear volume
    private BigDecimal prevTotalVolume; //previous bull volume + previous bear volume

    private String volumeSpike;

    private long timestamp;

    public VolumeTrendEnum getCurrentVolumeTrend() {
        if(currDivergence.compareTo(BigDecimal.ZERO) > 0) {
            return VolumeTrendEnum.BULL;
        }
        else if(currDivergence.compareTo(BigDecimal.ZERO) < 0) {
            return VolumeTrendEnum.BEAR;
        }
        else {
            return VolumeTrendEnum.SIDEWAYS;
        }
    }
}
