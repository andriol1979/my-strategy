package com.vut.mystrategy.service.strategy.indicator;

import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.num.Num;

@Slf4j
public class SchaffTrendCycleIndicator extends AbstractIndicator<Num> {

    private final SMAIndicator smoothedSTC;              // STC (SMA của %K2)

    public SchaffTrendCycleIndicator(BarSeries series) {
        //Use default parameters
        /*
        Tối ưu thông số STC cho scalping khung 15 phút:
            Scalping cần tín hiệu nhanh nhưng vẫn lọc nhiễu tốt, nên tao đề xuất thông số:
                - EMA ngắn: 10 (thay vì 23) → giúp MACD phản ứng nhanh hơn.
                - EMA dài: 21 (thay vì 50) → giảm độ trễ nhưng vẫn giữ được tín hiệu chính xác.
                - Stochastic MACD: 5 kỳ (thay vì 10) → tăng tốc độ phản ứng.
                - Smoothing EMA: 2 kỳ (thay vì 3) → tín hiệu nhanh hơn một chút.
                - Stochastic lần 2: 5 kỳ (thay vì 10) → giữ độ nhạy cao.
         */
        this(series, 10, 21, 5, 2, false);
    }

    public SchaffTrendCycleIndicator(BarSeries series,
                                     int shortEMAPeriod,
                                     int longEMAPeriod,
                                     int stochKPeriod,
                                     int smoothMAPeriod,
                                     boolean smoothBySMA) {
        super(series);
        // Bước 1: Tính EMA ngắn và dài
        // EMA ngắn (23 kỳ)
        EMAIndicator shortEMA = new EMAIndicator(new ClosePriceIndicator(series), shortEMAPeriod);
        // EMA dài (50 kỳ)
        EMAIndicator longEMA = new EMAIndicator(new ClosePriceIndicator(series), longEMAPeriod);

        // Bước 2: Tính MACD = EMA ngắn - EMA dài
        CombineIndicator macd = CombineIndicator.minus(shortEMA, longEMA);
        HighestValueIndicator macdHigh = new HighestValueIndicator(macd, stochKPeriod);
        LowestValueIndicator macdLow = new LowestValueIndicator(macd, stochKPeriod);

        // Bước 3: Tính %K của MACD (Stochastic lần 1, 10 kỳ)
        // %K của MACD
        StochasticMACDIndicator stochK1 = new StochasticMACDIndicator(macd, macdLow, macdHigh);

        // Bước 4: Làm mượt lần 1 - Tính %D = SMA hoặc EMA của %K
        Indicator<Num> stochD1 = smoothBySMA
                ? new SMAIndicator(stochK1, smoothMAPeriod)
                : new EMAIndicator(stochK1, smoothMAPeriod);

        HighestValueIndicator stochHigh = new HighestValueIndicator(stochD1, stochKPeriod);
        LowestValueIndicator stochLow = new LowestValueIndicator(stochD1, stochKPeriod);
        // Bước 5: Tính %K của %D (Stochastic lần 2, 10 kỳ)
        // %K của %D
        StochasticMACDIndicator stochK2 = new StochasticMACDIndicator(stochD1, stochLow, stochHigh);

        // Bước 6: Tính STC = SMA của %K2 (3 kỳ)
        this.smoothedSTC = new SMAIndicator(stochK2, smoothMAPeriod);
    }

    @Override
    public Num getValue(int index) {
        Num stcAtIndex = this.smoothedSTC.getValue(index); // Giá trị STC tại index
        log.info("STCIndicator - Index {} - STC at index: {}", index, stcAtIndex);
        return stcAtIndex;
    }

    @Override
    public int getUnstableBars() {
        return 0;
    }

    /**
     * Custom Stochastic MACD Indicator
     */
    static class StochasticMACDIndicator extends AbstractIndicator<Num> {
        private final Indicator<Num> macd;
        private final Indicator<Num> macdLow;
        private final Indicator<Num> macdHigh;

        public StochasticMACDIndicator(Indicator<Num> macd, Indicator<Num> macdLow, Indicator<Num> macdHigh) {
            super(macd.getBarSeries());
            this.macd = macd;
            this.macdLow = macdLow;
            this.macdHigh = macdHigh;
        }

        @Override
        public Num getValue(int index) {
            Num macdValue = macd.getValue(index);
            Num lowest = macdLow.getValue(index);
            Num highest = macdHigh.getValue(index);
            Num stochasticMACD = highest.minus(lowest).isZero() ? macd.numOf(50) // Tránh chia cho 0
                    : (macdValue.minus(lowest)).dividedBy(highest.minus(lowest)).multipliedBy(this.hundred());
            log.info("StochasticMACDIndicator - Index {} - StochasticMACD at index: {}", index, stochasticMACD);
            return stochasticMACD;
        }

        @Override
        public int getUnstableBars() {
            return 0;
        }
    }
}
