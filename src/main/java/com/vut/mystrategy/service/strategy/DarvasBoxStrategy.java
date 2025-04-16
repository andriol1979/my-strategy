package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.KlineIntervalEnum;
import com.vut.mystrategy.service.strategy.indicator.ClosedVolumeIndicator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.StopLossRule;

@Slf4j
public class DarvasBoxStrategy {
    public static Strategy buildStrategy(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);
        ClosedVolumeIndicator volume = new ClosedVolumeIndicator(series);
        DarvasBoxIndicator darvasBox = new DarvasBoxIndicator(series, highPrice, lowPrice, 3);

        Rule entryRule = new CrossedUpIndicatorRule(closePrice, darvasBox.getUpperBound())
                .and(new VolumeIncreaseRule(series, volume, 20));

        Rule exitRule = new CrossedDownIndicatorRule(closePrice, darvasBox.getLowerBound())
                .or(new StopLossRule(closePrice, 5));

        return new BaseStrategy("DarvasBoxStrategy", entryRule, exitRule);
    }

    static class DarvasBoxIndicator implements Indicator<Num> {
        private final BarSeries series;
        private final HighPriceIndicator highPrice;
        private final LowPriceIndicator lowPrice;
        private final int consolidationBars;
        @Getter
        private Num upperBound;
        @Getter
        private Num lowerBound;
        private int lastBoxIndex = -1;

        public DarvasBoxIndicator(BarSeries series, HighPriceIndicator highPrice, LowPriceIndicator lowPrice, int consolidationBars) {
            this.series = series;
            this.highPrice = highPrice;
            this.lowPrice = lowPrice;
            this.consolidationBars = consolidationBars;
            // Khởi tạo giá trị ban đầu từ nến đầu tiên
            if (series.getBarCount() > 0) {
                this.upperBound = highPrice.getValue(0);
                this.lowerBound = lowPrice.getValue(0);
            }
            updateBox(0); // Cập nhật hộp đầu tiên
        }

        @Override
        public Num getValue(int index) {
            if (index > lastBoxIndex) {
                updateBox(index);
            }
            return upperBound; // upperBound giờ luôn có giá trị
        }

        @Override
        public int getUnstableBars() {
            return 0;
        }

        @Override
        public BarSeries getBarSeries() {
            return series;
        }

        @Override
        public Num numOf(Number number) {
            return series.numOf(number);
        }

        private void updateBox(int index) {
            if (index + consolidationBars >= series.getBarCount()) {
                return; // Không đủ dữ liệu để cập nhật
            }

            Num high = highPrice.getValue(index);
            boolean isNewHigh = true;
            for (int i = 1; i <= consolidationBars; i++) {
                if (highPrice.getValue(index + i).isGreaterThan(high)) {
                    isNewHigh = false;
                    break;
                }
            }

            if (isNewHigh) {
                upperBound = high;
                lowerBound = high; // Reset lowerBound trước khi tìm giá trị thấp nhất
                for (int i = 1; i <= consolidationBars; i++) {
                    Num low = lowPrice.getValue(index + i);
                    if (low.isLessThan(lowerBound)) {
                        lowerBound = low;
                    }
                }
                lastBoxIndex = index + consolidationBars;
            }
            // Nếu không tìm thấy hộp mới, giữ nguyên upperBound và lowerBound cũ
        }
    }

    // Quy tắc kiểm tra volume tăng, thêm BarSeries để gọi numOf
    static class VolumeIncreaseRule extends AbstractRule {
        private final BarSeries series;
        private final ClosedVolumeIndicator volume;
        private final double minIncreasePercent;

        public VolumeIncreaseRule(BarSeries series, ClosedVolumeIndicator volume, double minIncreasePercent) {
            this.series = series;
            this.volume = volume;
            this.minIncreasePercent = minIncreasePercent;
        }

        @Override
        public boolean isSatisfied(int index, TradingRecord tradingRecord) {
            if (index < 1) return false;
            Num currentVolume = volume.getValue(index);
            Num previousVolume = volume.getValue(index - 1);
            Num increasePercent = currentVolume.minus(previousVolume)
                    .dividedBy(previousVolume)
                    .multipliedBy(series.numOf(100));
            return increasePercent.isGreaterThanOrEqual(DecimalNum.valueOf(minIncreasePercent));
        }
    }

    public static void main(String[] args) {
//        BarSeries series = BarSeriesLoader.loadFromCsv("backtest/1000SHIBUSDT_Binance_futures_UM_hour.csv");
        BarSeries series = BarSeriesLoader.loadFromDatabase(Constant.EXCHANGE_NAME_BINANCE, "btcusdt", KlineIntervalEnum.ONE_HOUR);
        Strategy strategy = buildStrategy(series);
        BarSeriesManager seriesManager = new BarSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        //print strategy
        LogMessage.printStrategyAnalysis(log, series, tradingRecord);
    }
}
